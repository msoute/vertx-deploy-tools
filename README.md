vertx-deploy-mod
================
# Introduction
vertx-deploy-mod

Tooling to deploy Vert.X modules, generic artifacts and (application) configuration onto cloud based instances (Ec2).

# Changelog
## DEV

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

    <deployConfiguration>
        <target>[target]</target>
        <testScope>false</testScope>
        <restart>true</restart>
        <elb>true</elb>
        <autoScaling>true</autoScaling>
        <autoScalingGroupId>autoscaling-group-id</autoScalingGroupId>
    </deployConfiguration>
    



