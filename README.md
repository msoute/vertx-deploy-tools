vertx-deploy-mod
================
# Introduction
vertx-deploy-mod

Tooling to deploy Vert.X modules, generic artifacts and (application) configuration onto cloud based instances (Ec2).

# Changelog
## DEV

## 1.1.21
* [Bug] Fixed deploy to autoscaling groups
* [Feature] removed `aws.as.deregister.decrementDesiredCapacity` config option from module. Moved it to deployConfiguration.

## 1.1.20
* [Feature] Change configuration option `aws.as.deregister.decrementDesiredCapacity` to default is true

## 1.1.19
* [Feature] Default to include instances that are in STANDBY. Instances can be excluded by setting `includeInStandby` to false in DeployConfiguration. 
* [Feature] Add configuration option `aws.as.deregister.decrementDesiredCapacity` to configure if desired capacity should be decremented if an instance is put in standby (defaults to false)

## 1.1.18
* [Feature] Add `aws.as.register.maxduration` and `aws.as.deregister.maxduration` to overwrite the default timeouts for (de)registration of an instance (autoscaling only). Timeouts are specified in minutes.

## 1.1.15
* [Minor] Add -mod to module zip.

## 1.1.14
* [Bug] Do not list elb's in an autoscaling group as a single string.
* [Bug] Ignore null values during xpath parsing for elb instance members.

## 1.1.12
* [Feature] Add configurable request timeout to maven execution ('-Ddeploy.requestTimeout'), defaults to 10 minutes

## Vert.X deploy mod
A Vert.X based module to deploy that is able to deploy other modules into the local Vert.X container. If configured it takes into account
ElasticLoadBalancers the instance is a member if to.


# Installation / Configuration

## Maven Plugin Configuration
    ...
    <plugin>
        <groupId>nl.jpoint.vertx-deploy-tools</groupId>
        <artifactId>vertx-deploy-maven-plugin</artifactId>
        <version>1.1.0-SNAPSHOT</version>
        <configuration>
            <deployConfigurations>
                ...
            </deployConfigurations>
        </configuration>
    </plugin>
    ...
### DeployConfigurations

Multipe targets can be configured. The target configuration can be selected with *-Ddeploy.target=*


## Vert.X Module Configuration


# Examples

## Deploy to multiple instances in an auto-scaling group with ELB support.
The following target configuration wil deploy to all instances in the as-group *autoscaling-group-id* and check if each instance reached the inService status
on the ELB's it is a member of. If the instance does not reach the inService status within 5 minutes the build wil fail and the remaining instances wil nog be updated

For deployment to groups the following extra take place :  

 * Check if an autoscaling group is in a deployable state (if not the deploy wil fail)
 * The following AS processes are suspended : "ScheduledActions", "Terminate", "ReplaceUnhealthy"
 * If minimum capacity is equal or greater than desired capacity the minimum capacity is changed to minimum - 1
 * Deploy to all instances in the as group. It will fail if the As Group reaches a state where it can not be guaranteed that at least one instance stays online
 * The minimum capacity is restored to its old value
 * All suspended processes are resumed.   
    
 Note :
   The decrementDesiredCapacity should be set to true (default:false). The autoscaling process Launch is not suspendend to make sure instances can exitStandby. 
   If the desiredCapacity is not decremented when an instance is put into standby the ASGroup wil launch a new instance.

    <deployConfiguration>
        <target>[target]</target>
        <!-- Deploy modules in scope test ->
        <testScope>false|true</testScope>
        <!-- Restarts all modules, not only those that are updated -->
        <restart>true</restart>
        <!-- Enable inService checks on elbs -->
        <elb>true</elb>
        <!-- Enable AS support -->
        <autoScaling>true</autoScaling>
        <!-- AS Group Id
        <autoScalingGroupId>autoscaling-group-id</autoScalingGroupId>
        <!-- Do not decrement desired capacity when an instance is put into standby.
        <decrementDesiredCapacity>false</decrementDesiredCapacity>
    </deployConfiguration>
    



