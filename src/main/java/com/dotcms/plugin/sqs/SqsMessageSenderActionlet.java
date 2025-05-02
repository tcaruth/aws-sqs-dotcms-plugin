package com.dotcms.plugin.sqs;

import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
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
            "The AWS region where the queue is located (e.g., us-east-1)", true));

        params.add(new WorkflowActionletParameter("delaySeconds", "Delay Seconds",
            "The number of seconds to delay the message (0-900). Default is 0.", false));

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
        try {
            // Extract parameters
            String queueUrl = params.get("queueUrl").getValue();
            String messageBody = params.get("messageBody").getValue();
            String awsRegion = params.get("awsRegion").getValue();

            // Process velocity variables in the message body if present
            if (messageBody != null && messageBody.contains("$")) {
                messageBody = processor.getContentlet().getMap().toString();
            }

            // Get optional delay seconds parameter
            int delaySeconds = 0;
            try {
                if (params.get("delaySeconds") != null && params.get("delaySeconds").getValue() != null) {
                    delaySeconds = Integer.parseInt(params.get("delaySeconds").getValue());
                    // SQS allows delay between 0-900 seconds
                    if (delaySeconds < 0 || delaySeconds > 900) {
                        delaySeconds = 0;
                    }
                }
            } catch (NumberFormatException e) {
                Logger.warn(this, "Invalid delay seconds value: " + params.get("delaySeconds").getValue() + ". Using default value 0.");
            }

            // Initialize SQS client
            Region region = Region.of(awsRegion);
            // TODO replace with Josten's credentials
            // Move to environment variables or configuration properties
            AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create("YOUR_ACCESS_KEY", "YOUR_SECRET_KEY")
            );
            SqsClient sqsClient = SqsClient.builder()
                .region(region)
                .credentialsProvider(credentialsProvider)
                .build();

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
            Logger.error(this, "Error sending message to SQS: " + e.getMessage(), e);
            throw new WorkflowActionFailureException("Failed to send message to SQS: " + e.getMessage());
        } catch (Exception e) {
            Logger.error(this, "Unexpected error executing SQS actionlet: " + e.getMessage(), e);
            throw new WorkflowActionFailureException("Unexpected error: " + e.getMessage());
        }
    }
}
