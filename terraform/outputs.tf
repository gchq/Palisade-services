output "kubeconfig" {
  value = "${local.kubeconfig}"
  description = "A generated sample configuration file to allow kubectl to connect to the EKS cluster. Use aws eks update-kubeconfig --name xxx to update the ~/./kube/config file"
}