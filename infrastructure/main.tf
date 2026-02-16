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