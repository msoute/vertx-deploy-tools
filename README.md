vertx-deploy-mod
================
# Introduction
vertx-deploy-mod

Tooling to deploy Vert.X modules, generic artifacts and (application) configuration onto cloud based instances (Ec2).

# Installation / Configuration

## Maven Plugin Configuration
    ...
    <plugin>
        <groupId>nl.jpoint.vertx-deploy-tools</groupId>
        <artifactId>vertx-deploy-maven-plugin</artifactId>
        <version>1.2.0-SNAPSHOT</version>
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
* **restart** : Restart whole container or only deployed modules (default : *false*)    
* **deploySnapshots** : Also deploy -SNAPSHOT artifacts (default : *false*)   

#### Aws Configuration Options
* **awsPrivateIp** : Use public or private ip's to connect to deploy mod. (default : *false*)    
* **UseOpsWorks** : Use OpsWorks (Layers) to detect instances to deploy to. Cannot be use icw *useAutoScaling*. (default : *false*)    
* **useAutoScaling** : Use auto scaling groups to detect instances to deploy to. Cannot be used icw *useOpsWorks*. (default : *false*)    
* **elb** : When true the deploy module with wait for instances to come InService on attached elb's before continuing deploy. (default : *false*)    

#### Aws AutoScaling Configuration Options
* **autoScalingGroupId** : The auto scaling group to get the list of instances from. 
* **ignoreInStandby** : When true, any instance that is in standby in the auto scaling group will also be added as host to deploy to. InStandby instances will always be deployed to first. (default : *false*)
* **ignoreDeployState** : Ignore if the application will go offline during a deploy and the InService count drops under the configured minimum instance count (default : *false*)
* **decrementDesiredCapacity** Decrement configured desired capacity with 1 to make sure that configured policies won't launch a new instance (default : *true*)
* **keepCurrentCapacity** : if true an extra instance will be added to the auto scaling group. The deploy continues after the new instance comes InService on Elb or auto scaling group. After the deploy one instance will be removed from the auto scaling group. (default : *true*)

#### Aws OpsWorks Configuration Options
* **String opsWorksLayerId** : The layer id to get a list of instances from.

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
    

# Changelog
## 1.2-SNAPSHOT

* [Feature] Add support for seamless deploy without capacity loss to autoscaling groups with option `keepCurrentCapacity` defaults to false
* [Feature] Add local run support `deploy.internal`.
* [Feature] Add support to deploy to autoscaling groups
* [Feature] Add 'aws.as.register.maxduration' and 'aws.as.deregister.maxduration' to overwrite the default timeouts for (de)registration of an instance (autoscaling only). Timeouts are specified in minutes.
* [Feature] Make aws region configurable `aws.region` (defaults to eu-west-1) 
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

## Vert.X deploy mod
A Vert.X based module to deploy that is able to deploy other modules into the local Vert.X container. If configured it takes into account
ElasticLoadBalancers the instance is a member if to.


