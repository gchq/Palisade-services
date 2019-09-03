# This data source is included for ease of sample architecture deployment
# and can be swapped out as necessary.

#Taken from here: https://learn.hashicorp.com/terraform/aws/eks-intro

terraform {
  required_version = ">= 0.12.0"
}

provider "aws" {
  version = ">= 2.11"
  region = var.region
}

data "aws_availability_zones" "available" {}


resource "aws_vpc" "palisade-eks-vpc" {
  //  # (resource arguments)
  cidr_block = "10.0.0.0/16"
  tags = "${
      map(
       "Name", "terraform-eks-demo-node",
       "kubernetes.io/cluster/${var.cluster-name}", "shared"
      )
    }"
}

resource "aws_subnet" "palisade-eks-subnet" {
  count = 2

  availability_zone = "${data.aws_availability_zones.available.names[count.index]}"
  cidr_block = "10.0.${count.index}.0/24"
  vpc_id = "${aws_vpc.palisade-eks-vpc.id}"

  tags = "${
    map(
     "Name", "terraform-eks-demo-node",
     "kubernetes.io/cluster/${var.cluster-name}", "shared"
    )
  }"
}

resource "aws_internet_gateway" "palisade-eks-internet-gateway" {
  vpc_id = "${aws_vpc.palisade-eks-vpc.id}"

  tags = {
    Name = "terraform-eks-demo"
  }
}

resource "aws_route_table" "palisade-eks-route-table" {
  vpc_id = "${aws_vpc.palisade-eks-vpc.id}"

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = "${aws_internet_gateway.palisade-eks-internet-gateway.id}"
  }
}

resource "aws_route_table_association" "demo" {
  count = 2

  subnet_id = "${aws_subnet.palisade-eks-subnet.*.id[count.index]}"
  route_table_id = "${aws_route_table.palisade-eks-route-table.id}"
}

resource "aws_iam_role" "palisade-cluster" {
  name = "terraform-eks-palisade-cluster"

  assume_role_policy = <<POLICY
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "eks.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
POLICY
}

resource "aws_iam_role_policy_attachment" "palisade-cluster-AmazonEKSClusterPolicy" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSClusterPolicy"
  role = "${aws_iam_role.palisade-cluster.name}"
}

resource "aws_iam_role_policy_attachment" "palisade-cluster-AmazonEKSServicePolicy" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSServicePolicy"
  role = "${aws_iam_role.palisade-cluster.name}"
}


resource "aws_security_group" "palisade-cluster-security-group" {
  name = "terraform-eks-demo-cluster"
  description = "Cluster communication with worker nodes"
  vpc_id = "${aws_vpc.palisade-eks-vpc.id}"

  egress {
    from_port = 0
    to_port = 0
    protocol = "-1"
    cidr_blocks = [
      "0.0.0.0/0"]
  }

  tags = {
    Name = "terraform-eks-palisade"
  }
}

# OPTIONAL: Allow inbound traffic from your local workstation external IP
#           to the Kubernetes. The IP addresses here represent the combined
#           list of those addresses that are allowed access.

resource "aws_security_group_rule" "palisade-cluster-ingress-workstation-https" {
  cidr_blocks = [
    "165.225.76.0/23",
    "165.225.80.0/22",
    "185.125.224.0/22",
    "195.92.40.49/32",
    "212.137.36.228/32",
    "34.240.141.198/32",
    "34.253.77.184/32",
    "35.176.136.170/32",
    "35.177.97.88/32",
    "35.178.132.230/32",
    "51.140.114.144/32",
    "51.140.78.31/32",
    "51.141.26.231/32",
    "51.141.34.27/32",
    "52.19.126.58/32",
    "62.25.106.209/32",
    "62.25.109.195/32",
    "62.25.109.202/32",
    "65.225.209.240/29",
    "62.254.157.222/32"]
  description = "Allow workstation to communicate with the cluster API Server"
  from_port = 443
  protocol = "tcp"
  security_group_id = "${aws_security_group.palisade-cluster-security-group.id}"
  to_port = 443
  type = "ingress"
}

resource "aws_eks_cluster" "palisade-eks-cluster" {
  name = "${var.cluster-name}"
  role_arn = "${aws_iam_role.palisade-cluster.arn}"

  vpc_config {
    security_group_ids = [
      "${aws_security_group.palisade-cluster-security-group.id}"]
    subnet_ids = "${aws_subnet.palisade-eks-subnet.*.id}"
  }

  depends_on = [
    "aws_iam_role_policy_attachment.palisade-cluster-AmazonEKSClusterPolicy",
    "aws_iam_role_policy_attachment.palisade-cluster-AmazonEKSServicePolicy",
  ]
}

//If you are planning on using kubectl to manage the Kubernetes cluster, now might be a great time to configure your client.
//After configuration, you can verify cluster access via kubectl version displaying server version information in addition to local client version information.
//The AWS CLI eks update-kubeconfig command provides a simple method to create or update configuration files.
//If you would rather update your configuration manually, the below Terraform output generates a sample kubectl configuration to connect to your cluster.
//This can be placed into a Kubernetes configuration file, e.g. ~/.kube/config

locals {
  kubeconfig = <<KUBECONFIG


apiVersion: v1
clusters:
- cluster:
    server: ${aws_eks_cluster.palisade-eks-cluster.endpoint}
    certificate-authority-data: ${aws_eks_cluster.palisade-eks-cluster.certificate_authority.0.data}
  name: kubernetes
contexts:
- context:
    cluster: kubernetes
    user: aws
  name: aws
current-context: aws
kind: Config
preferences: {}
users:
- name: aws
  user:
    exec:
      apiVersion: client.authentication.k8s.io/v1alpha1
      command: aws-iam-authenticator
      args:
        - "token"
        - "-i"
        - "${var.cluster-name}"
KUBECONFIG
}


resource "aws_iam_role" "palisade-eks-node-iam-role" {
  name = "terraform-eks-demo-node"

  assume_role_policy = <<POLICY
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "ec2.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
POLICY
}

resource "aws_iam_role_policy_attachment" "palisade-eks-node-AmazonEKSWorkerNodePolicy" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy"
  role = "${aws_iam_role.palisade-eks-node-iam-role.name}"
}

resource "aws_iam_role_policy_attachment" "palisade-eks-node-AmazonEKS_CNI_Policy" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy"
  role = "${aws_iam_role.palisade-eks-node-iam-role.name}"
}

resource "aws_iam_role_policy_attachment" "palisade-eks-node-AmazonEC2ContainerRegistryReadOnly" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
  role = "${aws_iam_role.palisade-eks-node-iam-role.name}"
}

resource "aws_iam_instance_profile" "palisade-eks-node-profile" {
  name = "palisade-eks-node-profile"
  role = "${aws_iam_role.palisade-eks-node-iam-role.name}"
}


resource "aws_security_group" "palisade-security-group-node" {
  name = "terraform-eks-demo-node"
  description = "Security group for all nodes in the cluster"
  vpc_id = "${aws_vpc.palisade-eks-vpc.id}"

  egress {
    from_port = 0
    to_port = 0
    protocol = "-1"
    cidr_blocks = [
      "0.0.0.0/0"]
  }

  tags = "${
    map(
     "Name", "terraform-eks-demo-node",
     "kubernetes.io/cluster/${var.cluster-name}", "owned"
    )
  }"
}

resource "aws_security_group_rule" "palisade-node-ingress-self" {
  description = "Allow node to communicate with each other"
  from_port = 0
  protocol = "-1"
  security_group_id = "${aws_security_group.palisade-security-group-node.id}"
  source_security_group_id = "${aws_security_group.palisade-security-group-node.id}"
  to_port = 65535
  type = "ingress"
}

resource "aws_security_group_rule" "palisade-node-ingress-cluster" {
  description = "Allow worker Kubelets and pods to receive communication from the cluster control plane"
  from_port = 1025
  protocol = "tcp"
  security_group_id = "${aws_security_group.palisade-security-group-node.id}"
  source_security_group_id = "${aws_security_group.palisade-cluster-security-group.id}"
  to_port = 65535
  type = "ingress"
}


//allow worker nodes networking access to the EKS master cluster
resource "aws_security_group_rule" "palisade-cluster-ingress-node-https" {
  description = "Allow pods to communicate with the cluster API Server"
  from_port = 443
  protocol = "tcp"
  security_group_id = "${aws_security_group.palisade-cluster-security-group.id}"
  source_security_group_id = "${aws_security_group.palisade-security-group-node.id}"
  to_port = 443
  type = "ingress"
}

//worker node autoscaling group
data "aws_ami" "eks-worker" {
  filter {
    name = "name"
    values = [
      "amazon-eks-node-${aws_eks_cluster.palisade-eks-cluster.version}-v*"]
  }

  most_recent = true
  owners = [
    "602401143452"]
  # Amazon EKS AMI Account ID
}


# This data source is included for ease of sample architecture deployment
# and can be swapped out as necessary.
data "aws_region" "current" {}

# EKS currently documents this required userdata for EKS worker nodes to
# properly configure Kubernetes applications on the EC2 instance.
# We implement a Terraform local here to simplify Base64 encoding this
# information into the AutoScaling Launch Configuration.
# More information: https://docs.aws.amazon.com/eks/latest/userguide/launch-workers.html
locals {
  demo-node-userdata = <<USERDATA
#!/bin/bash
set -o xtrace
/etc/eks/bootstrap.sh --apiserver-endpoint '${aws_eks_cluster.palisade-eks-cluster.endpoint}' --b64-cluster-ca '${aws_eks_cluster.palisade-eks-cluster.certificate_authority.0.data}' '${var.cluster-name}'
USERDATA
}

resource "aws_launch_configuration" "palisade-autoscaling-group" {
  associate_public_ip_address = true
  iam_instance_profile = "${aws_iam_instance_profile.palisade-eks-node-profile.name}"
  image_id = "${data.aws_ami.eks-worker.id}"
  instance_type = "m4.large"
  name_prefix = "terraform-eks-demo"
  security_groups = [
    "${aws_security_group.palisade-security-group-node.id}"]
  user_data_base64 = "${base64encode(local.demo-node-userdata)}"

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_autoscaling_group" "palisade-autoscaling-group" {
  desired_capacity = 2
  launch_configuration = "${aws_launch_configuration.palisade-autoscaling-group.id}"
  max_size = 2
  min_size = 1
  name = "terraform-eks-demo"
  vpc_zone_identifier = "${aws_subnet.palisade-eks-subnet.*.id}"

  tag {
    key = "Name"
    value = "terraform-eks-demo"
    propagate_at_launch = true
  }

  tag {
    key = "kubernetes.io/cluster/${var.cluster-name}"
    value = "owned"
    propagate_at_launch = true
  }
}
//
//The EKS service does not provide a cluster-level API parameter or resource to automatically configure the underlying Kubernetes cluster to allow worker nodes to join the cluster via AWS IAM role authentication.
//
//To output an example IAM Role authentication ConfigMap from your Terraform configuration:

locals {
  config_map_aws_auth = <<CONFIGMAPAWSAUTH


apiVersion: v1
kind: ConfigMap
metadata:
  name: aws-auth
  namespace: kube-system
data:
  mapRoles: |
    - rolearn: ${aws_iam_role.palisade-eks-node-iam-role.arn}
      username: system:node:{{EC2PrivateDNSName}}
      groups:
        - system:bootstrappers
        - system:nodes
CONFIGMAPAWSAUTH
}

output "config_map_aws_auth" {
  value = "${local.config_map_aws_auth}"
}

