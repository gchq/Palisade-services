#!/usr/bin/env bash

echo "cidr-access-group = ["
aws ec2 describe-security-groups --group-name its-corp-tcp-ssh --query "SecurityGroups[*].IpPermissions[*].IpRanges[*].CidrIp" | sed 's/[][ ]//g'
echo ","
aws ec2 describe-security-groups --group-name other-ip  --query "SecurityGroups[*].IpPermissions[*].IpRanges[*].CidrIp" | sed 's/[][ ]//g'

echo "]"
echo "cidr-block = \"10.0.0.0/16\""

echo "cluster-name = \"palisade-terraform-eks\""

