package com.dotcms.plugin.sqs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import org.apache.commons.io.IOUtils;
import com.dotcms.security.apps.AppSecretSavedEvent;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;

import com.dotmarketing.loggers.Log4jUtil;
import com.dotmarketing.osgi.GenericBundleActivator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.osgi.framework.BundleContext;

public class Activator extends GenericBundleActivator {

    private LoggerContext pluginLoggerContext;

    final LocalSystemEventsAPI localSystemEventsAPI = APILocator.getLocalSystemEventsAPI();

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        try {
            //Initializing log4j...
            try {
                LoggerContext dotcmsLoggerContext = Log4jUtil.getLoggerContext();
                pluginLoggerContext = (LoggerContext) LogManager
                        .getContext(this.getClass().getClassLoader(),
                                false,
                                dotcmsLoggerContext,
                                dotcmsLoggerContext.getConfigLocation());
                Logger.info(this.getClass().getName(), "Logger context initialized successfully");
            } catch (Exception e) {
                Logger.error(this.getClass().getName(), "Error initializing logger context at line " + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": " + e.getMessage(), e);
                throw e;
            }

            try {
                //Initializing services...
                initializeServices(bundleContext);
                Logger.info(this.getClass().getName(), "Services initialized successfully");
            } catch (Exception e) {
                Logger.error(this.getClass().getName(), "Error initializing services at line " + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": " + e.getMessage(), e);
                throw e;
            }

            try {
                //Registering the AWS SQS Actionlet
                registerActionlet(bundleContext, new SqsMessageSenderActionlet());
                Logger.info(this.getClass().getName(), "SQS Actionlet registered successfully");
            } catch (Exception e) {
                Logger.error(this.getClass().getName(), "Error registering SQS Actionlet at line " + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": " + e.getMessage(), e);
                throw e;
            }

            try {
                //Registering the AWS SQS App
                copyAppYml();
                CacheLocator.getAppsCache().clearCache();
                subscribeToAppSaveEvent();
            } catch (Exception e) {
                Logger.error(this.getClass().getName(), "Error setting up AWS SQS App at line " + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": " + e.getMessage(), e);
                throw e;
            }

            Logger.info(this.getClass().getName(), "AWS SQS Plugin started successfully");
        } catch (Exception e) {
            Logger.error(this.getClass().getName(), "FATAL ERROR starting AWS SQS Plugin: " + e.getMessage(), e);
            throw e;
        }
    }


    @Override
    public void stop(BundleContext context) throws Exception {
        Logger.info(this.getClass().getName(), "Stopping AWS SQS Plugin");

        unsubscribeToAppSaveEvent();
        deleteYml();

        //Unregister all the bundle services
        unregisterServices(context);

        //Shutting down log4j in order to avoid memory leaks
        Log4jUtil.shutdown(pluginLoggerContext);
    }

    private final File installedAppYaml = new File(ConfigUtils.getAbsoluteAssetsRootPath() + File.separator + "server"
                    + File.separator + "apps" + File.separator + AppKeys.APP_YAML_NAME);

    private void copyAppYml() throws IOException {

        // Check if parent directory exists and is writable
        File parentDir = installedAppYaml.getParentFile();
        if (!parentDir.exists()) {
            Logger.info(this.getClass().getName(), "Parent directory does not exist, attempting to create: " + parentDir.getAbsolutePath());
            if (!parentDir.mkdirs()) {
                throw new IOException("Failed to create parent directory: " + parentDir.getAbsolutePath());
            }
        }

        if (!parentDir.canWrite()) {
            throw new IOException("Cannot write to parent directory: " + parentDir.getAbsolutePath());
        }

        // Load the resource
        String resourcePath = "/" + AppKeys.APP_YAML_NAME;

        try (final InputStream in = this.getClass().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Resource not found: " + resourcePath + ". Make sure the file exists in the correct location in your bundle.");
            }

            IOUtils.copy(in, Files.newOutputStream(installedAppYaml.toPath()));

            // Verify the file was created
            if (!installedAppYaml.exists() || installedAppYaml.length() == 0) {
                throw new IOException("Failed to create or write to YAML file: " + installedAppYaml.getAbsolutePath());
            }

        } catch (IOException e) {
            Logger.error(this.getClass().getName(), "Error copying YAML file at line " + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": " + e.getMessage(), e);
            throw e;
        }

        try {
            CacheLocator.getAppsCache().clearCache();
        } catch (Exception e) {
            Logger.error(this.getClass().getName(), "Error clearing apps cache: " + e.getMessage(), e);
            throw new IOException("Error clearing apps cache: " + e.getMessage(), e);
        }
    }

    private void deleteYml() {
        if (installedAppYaml.exists()) {
            installedAppYaml.delete();
        }
    }

    private void subscribeToAppSaveEvent() {
        try {
            // Check if the API is available
            if (localSystemEventsAPI == null) {
                throw new IllegalStateException("LocalSystemEventsAPI is null. This could indicate a problem with API initialization.");
            }

            // Create the subscriber
            AppSecretEventSubscriber subscriber = new AppSecretEventSubscriber();

            // Subscribe to the event
            localSystemEventsAPI.subscribe(AppSecretSavedEvent.class, subscriber);
        } catch (Exception e) {
            Logger.error(this.getClass().getName(), "Error subscribing to App Save Event at line " + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": " + e.getMessage(), e);
            throw e; // Rethrow to ensure the activator knows about this failure
        }
    }

    private void unsubscribeToAppSaveEvent() {
        localSystemEventsAPI.unsubscribe(AppSecretEventSubscriber.class);
    }
}
