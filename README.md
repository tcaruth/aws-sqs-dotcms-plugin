# dotCMS AWS SQS Plugin

A dotCMS plugin that adds a workflow actionlet to send messages to Amazon SQS (Simple Queue Service) queues. This plugin integrates dotCMS content workflows with AWS SQS, enabling you to trigger messages to external systems when content is processed in dotCMS. It was built by referencing two example plugins from dotCMS:
- https://github.com/dotCMS/plugin-seeds/tree/master/com.dotcms.actionlet
- https://github.com/dotCMS/plugin-seeds/tree/master/com.dotcms.3rd.party

## How to build this example

To build the JAR, run the following Maven command: 
```sh
mvn clean install
```

This will generate the plugin JAR in the `target` directory.

## How to install this bundle

* **To install this bundle:**

  Upload the bundle JAR file using the dotCMS UI (`CMS Admin -> Plugins -> Upload Plugin`).

* **To uninstall this bundle:**

  Undeploy the bundle JAR using the dotCMS UI (`CMS Admin -> Plugins -> Undeploy`).

## How to create a Actionlet OSGi plugin

In order to create this OSGI plugin, Maven is configured to generate the `META-INF/MANIFEST.MF` file automatically. If needed, you can customize the configuration in the `pom.xml`.

Below is a description of the required fields in the `MANIFEST.MF` and how they are configured in a `pom.xml`:

> **Bundle-Name:** The name of your bundle  
> **Bundle-SymbolicName:** A short and unique name for the bundle  
> **Bundle-Vendor:** The vendor of the bundle (example: dotCMS)  
> **Bundle-Description:** A brief description of the bundle  
> **Bundle-DocURL:** URL for the bundle documentation  
> **Bundle-Activator:** Package and name of your Activator class (example: com.dotmarketing.osgi.actionlet.Activator)  
> **Export-Package:** Declares the packages that are visible outside the plugin. Any package not declared here has visibility only within the bundle.  
> **Import-Package:** This is a comma-separated list of the names of packages to import. This list must include the packages that you are using inside your OSGI bundle plugin and are exported and exposed by the dotCMS runtime.

These fields are configured in the `pom.xml` as follows:

```xml
<plugin>
    <groupId>org.apache.felix</groupId>
    <artifactId>maven-bundle-plugin</artifactId>
    <version>5.1.9</version>
    <extensions>true</extensions>
    <configuration>
        <instructions>
            <Bundle-Name>Your Bundle Name</Bundle-Name>
            <Bundle-SymbolicName>com.example.yourbundle</Bundle-SymbolicName>
            <Bundle-Vendor>dotCMS</Bundle-Vendor>
            <Bundle-Description>dotCMS - OSGI Actionlet example</Bundle-Description>
            <Bundle-DocURL>https://dotcms.com/</Bundle-DocURL>
            <Bundle-Activator>com.dotmarketing.osgi.actionlet.Activator</Bundle-Activator>
            <Export-Package>com.example.yourbundle.package</Export-Package>
            <Import-Package>*</Import-Package>
        </instructions>
    </configuration>
</plugin>
```

## Features

* Send messages to AWS SQS queues from dotCMS workflows
* Configure message body with content variables 
* Set message delivery delay (0-900 seconds)
* Specify AWS region for each queue

## Configuration Parameters

When configuring the workflow actionlet, you'll need to provide:

* **Queue URL** - The complete URL of your SQS queue
* **Message Body** - Content of the message (can include velocity variables)
* **AWS Region** - Region where your SQS queue is located
* **Delay Seconds** - (Optional) Delay for message delivery (0-900 seconds)

## Implementation

The plugin uses the AWS Java SDK v2 to interact with SQS. The main actionlet class handles the workflow integration while the activator registers it with dotCMS.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
