# ============================================================
# Variáveis do Terraform
# Os valores reais ficam em terraform.tfvars (não commitado).
# Veja terraform.tfvars.example para o template.
# ============================================================

variable "aws_region" {
  description = "Região AWS onde os recursos serão criados"
  type        = string
  default     = "us-east-1"
}

variable "db_username" {
  description = "Usuário administrador do banco RDS"
  type        = string
  sensitive   = true
}

variable "db_password" {
  description = "Senha do banco RDS. Use pelo menos 16 caracteres."
  type        = string
  sensitive   = true
}

variable "db_name" {
  description = "Nome do banco de dados PostgreSQL"
  type        = string
  default     = "sentinel_cloud"
}

variable "key_name" {
  description = "Nome do key pair EC2 para acesso SSH"
  type        = string
}

variable "allowed_ssh_cidr" {
  description = "CIDR do seu IP pessoal para acesso SSH. Ex: '177.10.20.30/32'. Use /32 para um IP único. NUNCA use 0.0.0.0/0 em produção."
  type        = string
}

variable "rds_publicly_accessible" {
  description = "Se true, o RDS fica acessível pela internet. Mantenha false em produção."
  type        = bool
  default     = false
}
