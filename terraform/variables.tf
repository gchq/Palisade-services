variable "region" {
  default = "eu-west-1"
}

variable "cluster-name" {
  default = "palisade-terraform-eks"
  type = "string"
}


variable "bucket" {
  default = "terraform-infra-palisade"
  type = "string"
}


variable "key" {
  default = "terraform/dev/terraform_dev.tfstate"
  type = "string"
}
