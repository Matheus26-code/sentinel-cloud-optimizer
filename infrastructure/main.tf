# main.tf - Margem Zero de Erro
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = "us-east-1" # Região da Virgínia (EUA)
}

# Vamos criar um "Cofre" (S3 Bucket) para guardar arquivos do projeto futuramente
resource "aws_s3_bucket" "sentinel_storage" {
  bucket = "sentinel-cloud-optimizer-storage-${random_id.suffix.hex}"
}

resource "random_id" "suffix" {
  byte_length = 4
}

data "aws_ami" "ubuntu" {
  most_recent = true
  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-focal-20.04-amd64-server-*"]
  }
  owners = ["099720109477"] # Canonical
}

# Criando a Instância EC2
resource "aws_instance" "sentinel_server" {
  ami           = data.aws_ami.ubuntu.id
  instance_type = "t2.micro" # Certifique-se de que us-east-1 suporta t2.micro no Free Tier

  tags = {
    Name = "Sentinel-Backend-Server"
  }
}

# Criando o Banco de Dados PostgreSQL (Free Tier)
resource "aws_db_instance" "sentinel_db" {
  allocated_storage    = 20
  engine               = "postgres"
  engine_version       = "16.3"
  instance_class       = "db.t3.micro"
  db_name              = "sentinel_cloud"
  username             = "sentinel_admin"
  password             = "sentinel123" # Use uma senha forte
  parameter_group_name = "default.postgres16"
  skip_final_snapshot  = true
  publicly_accessible  = true
}

# Exibindo o endereço do banco após a criação
output "rds_endpoint" {
  value = aws_db_instance.sentinel_db.endpoint
}

# Criando uma regra de segurança para permitir conexões no banco
resource "aws_security_group_rule" "allow_postgres" {
  type              = "ingress"
  from_port         = 5432
  to_port           = 5432
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"] # Em produção, use apenas o IP da sua EC2
  security_group_id = tolist(aws_db_instance.sentinel_db.vpc_security_group_ids)[0]
}

resource "aws_security_group_rule" "allow_java_backend" {
  type              = "ingress"
  from_port         = 8080
  to_port           = 8080
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
  security_group_id = tolist(aws_instance.sentinel_server.vpc_security_group_ids)[0]
}