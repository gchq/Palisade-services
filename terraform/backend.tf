terraform {
  backend "s3" {
    bucket = "terraform-infra-palisade"
    key = "terraform/dev/terraform_dev.tfstate"
    region = "eu-west-1"
  }
}