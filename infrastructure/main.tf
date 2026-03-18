provider "aws" {
  region = "us-east-1"
}

# 1. Grupo de Segurança para a Instância EC2 (Backend)
resource "aws_security_group" "sentinel_sg" {
  name        = "sentinel-sg-final"
  description = "Security group for Sentinel Cloud Optimizer"

  # Porta 22: SSH (Para o Deploy do GitHub e seu acesso manual)
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Porta 8080: Java Backend (Para o seu navegador/API)
  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Saída liberada para o servidor baixar atualizações
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# 2. Grupo de Segurança para o Banco de Dados RDS
resource "aws_security_group" "rds_sg" {
  name        = "sentinel-rds-sg"
  description = "Allow port 5432 for PostgreSQL"

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.sentinel_sg.id]
  }
}

# 3. Instância EC2 (Onde o Java vai rodar)
resource "aws_instance" "sentinel_server" {
  ami           = "ami-080e1f13689e07408" # Ubuntu 22.04 LTS em us-east-1
  instance_type = "t2.micro"
  

  key_name      = "sentinel-deploy-final"

  vpc_security_group_ids = [aws_security_group.sentinel_sg.id]

  tags = {
    Name = "sentinel-backend-server"
  }
}

# 4. Banco de Dados RDS (PostgreSQL)
resource "aws_db_instance" "sentinel_db" {
  allocated_storage    = 20
  db_name              = "sentinel_cloud"
  engine               = "postgres"
  engine_version       = "16.3"
  instance_class       = "db.t4g.micro"
  username             = "sentinel_admin"
  password             = "sentinel123"
  parameter_group_name = "default.postgres16"
  skip_final_snapshot  = true
  publicly_accessible  = true
  vpc_security_group_ids = [aws_security_group.rds_sg.id]
}

# Outputs para facilitar sua vida
output "ec2_public_ip" {
  value = aws_instance.sentinel_server.public_ip
}

output "rds_endpoint" {
  value = aws_db_instance.sentinel_db.endpoint
}