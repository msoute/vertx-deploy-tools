![Vertx Deploy Logo](./vertx-deploy-tools.png)

# Introduction

Tooling to deploy Vert.X applications, generic artifacts and (application) configuration onto cloud based instances (Ec2).

# Installation / Configuration

## Installation.
Install as any other application in with max 1 verticle instance.
Init.d scripts are provided for redhad and debian based systems. install as vertx (debian|redhat.vertx) and vertx-deploy (debian|redhat.vertx.deploy).
Place defaults in /etc/defaults/vertx and configure as needed.

The configured system user needs sudo access to the init.d vertx script and any (test) command. The user also needs write access to directories configured for artifacts

## Configuration

    {
        "vertx.home":"/opt/sw/vertx/current",
        "artifact.storage": "/opt/data/repo/",
        "http.authUser": "...",
        "http.authPass": "...",
        "maven.repo.uri": "...",
        "aws.auth.access.key":"****************",
        "aws.auth.secret.access.key":"**********************",
        "aws.region", "eu-west-1",
        "aws.as.register.maxduration":10,
    }
    
* **vertx.home** : The Vertx installation directory. (required)
* **vertx.run** : Dir where the applications writes state files for currently deployed applications (default {vertx.home}/run/)
* **artifact.storage** : Directory to download (config) artifacts to. (required)
* **http.authUser** : Nexus repo user 
* **http.authPass** : Nexus repo password
* **maven.repo.uri*** : Maven repo url
* **maven.repo.snapshot.policy**  : Maven snapshot policy (default: always)
* **config.location** : Location of config file (used for -conf when a verticle is instantiated)
* **aws.auth.access.key** : Aws access key
* **aws.auth.secret.access.key** Aws secret access key
* **aws.region** : The Aws region
* **aws.as.register.maxduration** : maximum (de)register duration in minutes (default:4)
* **vertx.default.java.opts** : Default java opts passed to the application with --java-opts (default "")
* **vertx.clustering** : (boolean) Enables -cluster

# Deploy configuration 
## Deploy artifacts.
The plugin uses the projects pom to determine what artifacts need to be deployed. There are three types of artifacts (in the order of deployment)
* config : configuration files, unpacked to a specified location
* static artifacts : static content, sites etc that are extracted to a specified location on the target host
* applications : Vert.x applications.

To instruct the plugin to deploy an artifact (application, artifact or config) it only needs to be added as a dependency to the pom.xml. The plugin ignores all dependencies specified in parent poms or transitive.

### Vert.x Application artifact
    ...
    <dependency>
        <groupId>[groupId]</groupId>
        <artifactId>[artifactId]</artifactId>
        <version>[version]</version>
    </dependency>
    ...

Applications are started using the vert.x CLI start method. all non config or static artifacts are regarded as vert.x applications.

### Config artifact

    <dependency>
        <groupId>[groupId]</groupId>
        <artifactId>[artifactId]</artifactId>
        <version>[version]</version>
        <classifier>[classifier]</version>
        <type>config</type>
    </dependency>

These artifacts will be unpacked to the location specified in artifact_context.xml (See appendix creating an artifact). Always before static artifacts or applications
identified by type config

### Static artifact

    <dependency>
        <groupId>[groupId]</groupId>
        <artifactId>[artifactId]</artifactId>
        <version>[version]</version>
        <classifier>site</version>
        <type>zip</type>
    </dependency>

These artifacts will be unpacked to the location specified in artifact_context.xml (See appendix creating an artifact). Always after config artifacts and before applications
identified by classifier site

## Maven Plugin Configuration
    ...
    <plugin>
        <groupId>nl.jpoint.vertx-deploy-tools</groupId>
        <artifactId>vertx-deploy-maven-plugin</artifactId>
        <version>3.0.0-SNAPSHOT</version>
        <configuration>
            <region>eu-west-1</region>
            <credentialsId>credentials</credentialsId>
            <port>6789</port>
            <deployConfigurations>
                ...
            </deployConfigurations>
        </configuration>
    </plugin>
    ...
    
### AWS Credendials
Aws credentials (accessKey / secretKey) should be stored in settings(-security).xml as a server element.

    <server>
        <id>credentialsId</id>
        <username>[accessKey]</username>
        <password>[secretKey]</password>	
    </server>

### DeployConfigurations

Multiple targets can be configured. The target configuration can be selected with *-Ddeploy.target=*

#### Default Deploy Configuration Options
* **target** : The target id
* **hosts** : List of hosts to deploy to. This property is ignored when deploying when either  *useOpsWorks* or *useAutoScaling* is true
* **deployConfig** : Indicates if config artifacts should also be deployed (default : *true*)
* **exclusions** : List of artifacts that should be ignored 
* **testScope** : Include artifacts in test scope (default : *false*) 
* **restart** : Restart whole container or only deployed applications (default : *false*)
* **deploySnapshots** : Also deploy -SNAPSHOT artifacts (default : *false*)


#### Aws Configuration Options
* **awsPrivateIp** : Use public or private ip's to connect to deploy application. (default : *false*)
* **UseOpsWorks** : Use OpsWorks (Layers) to detect instances to deploy to. Cannot be use icw *useAutoScaling*. (default : *false*)    
* **useAutoScaling** : Use auto scaling groups to detect instances to deploy to. Cannot be used icw *useOpsWorks*. (default : *false*)    
* **elb** : When true the deploy application with wait for instances to come InService on attached elb's before continuing deploy. (default : *false*)

#### Aws AutoScaling Configuration Options
* **autoScalingGroupId** : The auto scaling group to get the list of instances from. 
* **ignoreInStandby** : When true, any instance that is in standby in the auto scaling group will also be added as host to deploy to. InStandby instances will always be deployed to first. (default : *true*)
* **decrementDesiredCapacity** Decrement configured desired capacity with 1 to make sure that configured policies won't launch a new instance (default : *true*)
* **maxCapacity** : If Strategy is KEEP_CAPACITY, the capacity of the group wil never grow greater than **maxCapacity**. Defaults to max capacity in configured in auto scaling group.
* **minCapacity** : If Strategy is GUARANTEE_MINIMUM and a deploy failed the build wil also fail if the capacity drops under the configured minimum. (default : *1*)
* **deployStrategy** : The deploy strategy to use. Valid values are *KEEP_CAPACITY*, *GUARANTEE_MINIMUM*, *WHATEVER*. (default: *KEEP_CAPACITY) 

#### Auto Scaling deploy strategies.

* **KEEP_CAPACITY** : The deploy applications wil make sure the auto scale capacity wil not drop during the deploy. Before a deploy an extra instances will be added to the auto scaling group if the desired count is smaller than the auto scaling group
configured maximum. If *maxCapacity** is configured the desired count wil never be greater than **maxCapacity**. If *elb** is true the current InService count wil be based on the number of instances InService on the elb(s), otherwise the healthy instance count in the elb is used. 
* **DEFAULT** : The deploy applications wil only deploy if the  InService count is greater than the groups minimum count. The deploy wil never continue after a failed deploy. The deploy mod
wil not guarantee at least one InService instance (i.e. if the groups minimum count is 0 with one InService instance)
* **GUARANTEE_MINIMUM** : Yhe deploy applications does not care if a single instance deploy fails. As long as the InService count never drops below **minCapacity**. With **elb** the InService count on the elb wil be used. Otherwise the auto scaling group healthy count.
* **WHATEVER** : Kittens may die (a.k.a. you don't care, so we don't either, the application may go offline.)

Note : When there are no InService instances (elb or auto scaling group) on start of the deploy the strategy wil set to **WHATEVER**

#### Aws OpsWorks Configuration Options
* **String opsWorksLayerId** : The layer id to get a list of instances from.

### Mojos
General Mojo parameters

* **deploy.activeTarget** : selects the target configuration
* **deploy.requestTimeout** : overwrites the default timeout (in minutes)

#### mvn deploy:deploy

The default mojo. Based on configuration it wil deploy to a set of configured instances, instances in an OpWorks layer or instances in an auto scaling group.
When deploying to an OpsWorks based set of instances the deploy application can be configured to wait for every instance to come InService on an elb before continuing.

During deploys to an auto scaling group the plugin wil first try to deploy Standby instances and put those back into service. This can be overridden with **ignoreInStandby**

 When an instance is put InStandby for deployment it wil decrement the desired capacity of the auto scaling group with 1. This can be overridden (  **decrementDesiredCapacity** ). Note that an
auto scaling group will always try to conform to the configured desired amount i.e. it wil launch a new instance.

 Note : During a deploy to an auto scaling group the deploy application temporarily suspends the following auto scaling policies : *ScheduledActions* , *Terminate* and *ReplaceUnhealthy*. These processes are resumed
after the deploy finished (or fails). If the maven task is killed during the deploy those processes are not automatically. This can be fixed either by running a new deploy or with the AWS CLI  

 Note : The plugin assumes that a deploy is ready when an instance reaches the status *InService* on all elb's or when no elb's are attached in the auto scaling group. The default ELB health check
does not guarantee any application to be ready and listening. A custom elb health-check needs to be [configured](http://docs.aws.amazon.com/ElasticLoadBalancing/latest/DeveloperGuide/elb-healthchecks.html). 

Maven options :

#### mvn deploy:deploy-direct

Deploys to a single instance. For example when an instance comes online tools like puppet can instruct a remote Jenkins instance to schedule a build to deploy a set of modules to the newly created instance. 
This mojo ignores **deploy.activeTarget** 

* **deploy.remoteIp** : The Ip to deploy to *required* .
* **deploy.testScope** : Include test artifacts  (default : *false*)
* **deploy.withConfig** : Include config artifacts (default : *true*)

####  mvn deploy:deploy-single
Deploys a single artifact to a DeployTarget 

* **deploy.single.type** : artifact type *required*
* **deploy.single.groupId** : The artifact groupId *required*
* **deploy.single.artifactId** : The artifactId *required*
* **deploy.single.classifier** : The artifact classifier 
* **deploy.single.version** : The artifact version *required*

#### mvn deploy-single-as
Deploys to an autoscaling group

TODO

### mvn deploy:as-enable
Mojo to add one instance to an as_group if current instances size equals 0.
* **autoScalingGroupId** : The auto scaling group to enable. 

## Vert.X Module Configuration


# Examples

## Deploy to multiple instances in an auto-scaling group with ELB support.
The following target configuration wil deploy to all instances in the as-group *autoscaling-group-id* and check if each instance reached the inService status
on the ELB's it is a member of. If the instance does not reach the inService status within 5 minutes the build wil fail and the remaining instances wil nog be updated

    <deployConfiguration>
        <target>[target]</target>
        <testScope>false</testScope>
        <restart>true</restart>
        <elb>true</elb>
        <autoScaling>true</autoScaling>
        <autoScalingGroupId>autoscaling-group-id</autoScalingGroupId>
    </deployConfiguration>

# Creating (config) artifacts.
Artifacts other than vert.x applications can be constructed in a similar way by using the maven-assembly-plugin. These artifacts are extracted on the targeted instance. A
config file in the artifact root dir (artifact_context.xml) instructs the application where and how to extract the artifact.

    <?xml version="1.0"?>
    <artifact>
        <baselocation>...</baselocation>
        <testCommand>...</testCommand>
        <restartCommand>...</restartCommand>
        <checkContent>true|false</checkContent>
    </artifact>
    
* **baselocation** : Instructs the application where to extract the artifact. All existing dir's and / or files wil be removed first *required*
* **testCommand** : Runs a console command after extraction, if the command failed the build wil fail (i.e. nginx -t)
* **restartCommand** : Command to restart a service after extraction, runs after testCommand (i.e. service nginx restart)
* **checkContent** : Checks if the content in an artifact has changed (i.e. property file content) and forces a container restart.

# Phone Home

Applications deployed through the deploy application should report to the deploy application if de deploy was successful or failed. All verticles can do this by
sending a request to http://localhost:[port]/deploy/update?id=[id]&status=[ok|error]&errormessage=[message]

The deploy application adds an JVM property that indicates on what port the deploy application is running (vertxdeploy.port)

If an application reports an error the deploy wil fail, the same error is reported back to the maven pluging.

# Changelog

## 3.0.0-SNAPSHOT

* [Upgrade] Move to vertx.3
* [Feature] Add phone home function
* [Feature] Auto discover latest deployed version from auto scaling group and deploy on first start of deploy application
* [Maven Plugin] Only log result messages once
* [Maven Plugin] Fail builds if deploy of any instance failes for strategy WHATEVER and DEFAULT

## 1.2.1

* [Feature] Fail build if module is not reachable on any host to deploy to.

## 1.2.0

* [Feature] Auto remove InStandby instances from an autoscaling group if the instances has already been terminated (#11).
* [Feature] Add support to check if config has changed and force a restart of the container.
* [Feature] Add support for seamless deploy without capacity loss to autoscaling groups with option `keepCurrentCapacity` defaults to true.
* [Feature] Add local run support `deploy.internal`.
* [Feature] Add support to deploy to autoscaling groups.
* [Feature] Add 'aws.as.register.maxduration' and 'aws.as.deregister.maxduration' to overwrite the default timeouts for (de)registration of an instance (autoscaling only). Timeouts are specified in minutes.
* [Feature] Make aws region configurable `aws.region` (defaults to eu-west-1). 
* [Feature] Default to include instances that are in STANDBY. Instances can be excluded by setting `includeInStandby` to false in DeployConfiguration.
* [Feature] Add configuration option `aws.as.deregister.decrementDesiredCapacity` to configure if desired capacity should be decremented if an instance is put in standby (defaults to true)

* [Maven Plugin] Dropped custom Aws implementation in favor of Amazon SDK
* [Maven Plugin] Make port configurable, defaults to 6789
* [Maven Plugin] Make aws region configurable, defaults to eu-west-1

* [Vertx Module] Dropped custom Aws implementation in favor of Amazon SDK
* [Vertx Module] Make port configurable `http.port` , defaults to 6789
* [Vertx Module] Make aws region configurable, defaults to eu-west-1

Note 
Source level changed to 1.8

## 1.1.15
* [Minor] Add -mod to module zip.

## 1.1.14
* [Bug] Do not list elb's in an autoscaling group as a single string.
* [Bug] Ignore null values during xpath parsing for elb instance members.

## 1.1.12
* Add configurable request timeout to maven execution ('-Ddeploy.requestTimeout'), defaults to 10 minutes