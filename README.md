vertx-deploy-mod
================
# Introduction
vertx-deploy-mod

Tooling to deploy Vert.X modules, generic artifacts and (application) configuration onto cloud based instances (Ec2).

# Changelog
## 1.2-SNAPSHOT
### Add local run support.
## 1.1.12
### Add configurable request timeout to maven execution ('-Ddeploy.requestTimeout'), defaults to 10 minutes

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
    



