package com.dotcms.plugin.sqs;

import com.dotcms.security.apps.AppSecrets;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.dotcms.security.apps.AppsAPI;
import com.dotcms.security.apps.Secret;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
// AWS SQS Imports
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;

/**
 * SQS Message Sender Actionlet
 * This actionlet sends messages to an AWS SQS queue as part of a dotCMS workflow.
 */
public class SqsMessageSenderActionlet extends WorkFlowActionlet {

    private static final long serialVersionUID = 1L;

    @Override
    public List<WorkflowActionletParameter> getParameters() {
        List<WorkflowActionletParameter> params = new ArrayList<>();

        params.add(new WorkflowActionletParameter("queueUrl", "Queue URL",
            "The complete URL of the SQS queue", true));

        params.add(new WorkflowActionletParameter("messageBody", "Message Body",
            "The content of the message to send. You can use velocity variables like $content.title", true));

        params.add(new WorkflowActionletParameter("awsRegion", "AWS Region",
            "eu-north-1", true));

        params.add(new WorkflowActionletParameter("delaySeconds", "Delay Seconds (0-900)",
            "0", false));

        return params;
    }

    @Override
    public String getName() {
        return "Send Message to AWS SQS";
    }

    @Override
    public String getHowTo() {
        return "This actionlet sends a message to an AWS SQS queue. You need to specify the queue URL, message body and AWS region. You can optionally specify a delay in seconds for the message delivery.";
    }

    @Override
    public void executeAction(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params) throws WorkflowActionFailureException {
        // Declare variables at the method level so they're accessible in all blocks
        String queueUrl = null;
        String messageBody = null;
        String awsRegion = null;
        int delaySeconds = 0;
        SqsClient sqsClient = null;

        try {
            // SECTION 1: Extract and process parameters
            try {
                // Extract parameters
                queueUrl = params.get("queueUrl").getValue();

                messageBody = params.get("messageBody").getValue();

                awsRegion = params.get("awsRegion").getValue();

                // Use standard message body if none provided
                if (messageBody == null || messageBody.length() == 0) {
                    messageBody = processor.getContentlet().getMap().toString();
                }

                // Get optional delay seconds parameter
                try {
                    if (params.get("delaySeconds") != null && params.get("delaySeconds").getValue() != null) {
                        String delaySecondsStr = params.get("delaySeconds").getValue();
                        delaySeconds = Integer.parseInt(delaySecondsStr);
                        // SQS allows delay between 0-900 seconds
                        if (delaySeconds < 0 || delaySeconds > 900) {
                            Logger.warn(this, "Delay seconds value out of range: " + delaySeconds + ". Setting to default 0.");
                            delaySeconds = 0;
                        }
                    }
                } catch (NumberFormatException e) {
                    String delayValue = params.get("delaySeconds") != null ? params.get("delaySeconds").getValue() : "null";
                    Logger.warn(this, "Invalid delay seconds value: '" + delayValue + "'. Using default value 0. Error: " + e.getMessage());
                }
            } catch (Exception e) {
                Logger.error(this, "Error processing parameters at line " + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": " + e.getMessage(), e);
                throw new WorkflowActionFailureException("Error processing parameters: " + e.getMessage() + ". Queue: " + queueUrl + ", Region: " + awsRegion);
            }

            // SECTION 2: Initialize SQS client
            try {
                // Initialize SQS client
                Region region = Region.of(awsRegion);

                Logger.info(this, "Retrieving AWS credentials from dotCMS app configuration");

                try {
                    // Get the app service
                    AppsAPI appsAPI = APILocator.getAppsAPI();

                    // Get the AWS credentials from the app configuration
                    Optional<AppSecrets> appSecrets = appsAPI.getSecrets(AppKeys.APP_KEY, APILocator.systemHost(), APILocator.systemUser());

                    if (!appSecrets.isPresent()) {
                        throw new IllegalStateException("AWS SQS app secrets not found. Please configure the AWS credentials in the dotCMS app configuration.");
                    }

                    // Get the app configuration from the secrets
                    Map<String, Secret> secretsMap = appSecrets.get().getSecrets();

                    // Get the AWS credentials from the secrets map
                    Secret accessKeySecret = secretsMap.get(AppKeys.AWS_ACCESS_KEY.key);
                    Secret secretKeySecret = secretsMap.get(AppKeys.AWS_SECRET_KEY.key);

                    if (secretsMap == null || secretsMap.isEmpty()) {
                        throw new IllegalStateException("AWS SQS app configuration is empty. Please check the app configuration.");
                    }

                    String awsAccessKey = accessKeySecret != null ? new String(accessKeySecret.getValue()) : null;
                    String awsSecretKey = secretKeySecret != null ? new String(secretKeySecret.getValue()) : null;

                    if (awsAccessKey == null || awsAccessKey.isEmpty()) {
                        throw new IllegalStateException("AWS Access Key is missing or empty. Please configure it in the dotCMS app configuration.");
                    }

                    if (awsSecretKey == null || awsSecretKey.isEmpty()) {
                        throw new IllegalStateException("AWS Secret Key is missing or empty. Please configure it in the dotCMS app configuration.");
                    }

                    // Log partial key for debugging (never log full secret keys)
                    Logger.info(this, "Using AWS access key: " + awsAccessKey.substring(0, Math.min(4, awsAccessKey.length())) + "...");

                    AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(awsAccessKey, awsSecretKey)
                    );

                    sqsClient = SqsClient.builder()
                        .region(region)
                        .credentialsProvider(credentialsProvider)
                        .build();
                } catch (Exception e) {
                    Logger.error(this, "Error initializing SQS client at line " + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": " + e.getMessage(), e);
                    throw new WorkflowActionFailureException("Error initializing SQS client: " + e.getMessage() + ". Region: " + awsRegion);
                }
            } catch (Exception e) {
                throw new WorkflowActionFailureException("Error initializing SQS client: " + e.getMessage() + ". Region: " + awsRegion);
            }

            // SECTION 3: Send message to SQS
            try {
                // Create send message request
                SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageBody)
                    .delaySeconds(delaySeconds)
                    .build();

                // Send message to SQS queue
                SendMessageResponse response = sqsClient.sendMessage(sendMsgRequest);
                Logger.info(this, "Message sent to SQS queue. MessageId: " + response.messageId());
            } catch (SqsException e) {
                Logger.error(this, "Error sending message to SQS at line " + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": " + e.getMessage() + ". Queue: " + queueUrl, e);
                throw new WorkflowActionFailureException("Failed to send message to SQS: " + e.getMessage() + ". Queue: " + queueUrl);
            } catch (Exception e) {
                Logger.error(this, "Unexpected error sending message at line " + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": " + e.getMessage() + ". Queue: " + queueUrl, e);
                throw new WorkflowActionFailureException("Unexpected error sending message: " + e.getMessage() + ". Queue: " + queueUrl);
            } finally {
                // Close the SQS client if it was created
                if (sqsClient != null) {
                    try {
                        sqsClient.close();
                    } catch (Exception e) {
                        Logger.warn(this, "Error closing SQS client: " + e.getMessage());
                    }
                }
            }
        } catch (WorkflowActionFailureException e) {
            // Re-throw workflow exceptions that we've already created
            throw e;
        } catch (Exception e) {
            // Catch any other unexpected exceptions
            Logger.error(this, "FATAL ERROR in SQS actionlet at line " + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": " + e.getMessage() + ". Queue: " + queueUrl + ", Region: " + awsRegion, e);
            throw new WorkflowActionFailureException("Unexpected error in SQS actionlet: " + e.getMessage() + ". Queue: " + queueUrl + ", Region: " + awsRegion);
        }
    }
}
