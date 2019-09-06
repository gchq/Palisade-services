variable "region" {
  default = "eu-west-1"
}

variable "cluster-name" {
  type = "string"
}


variable "cidr-block" {
  default = "10.0.0.0/16"
  type = "string"
}

variable "cidr-block-subnet" {
  default = "10.0."
  type = "string"
}

variable "cidr-access-group" {
  type = "list"
}