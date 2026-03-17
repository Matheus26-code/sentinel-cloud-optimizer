provider "aws" {
  region = var.aws_region
}

# Busca a AMI mais recente do Ubuntu 22.04 LTS automaticamente.
# Evita hardcodar o ID da AMI, que muda por região e é atualizado periodicamente.
data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"] # Canonical (publisher oficial do Ubuntu)

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

# 1. Grupo de Segurança para a Instância EC2 (Backend)
resource "aws_security_group" "sentinel_sg" {
  name        = "sentinel-sg-final"
  description = "Security group for Sentinel Cloud Optimizer"

  # Porta 22: SSH restrito ao IP do desenvolvedor.
  # NUNCA use 0.0.0.0/0 — expõe o servidor a ataques de força bruta.
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.allowed_ssh_cidr]
  }

  # Porta 8080: Java Backend
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

  tags = {
    Name        = "sentinel-backend-sg"
    Project     = "sentinel-cloud-optimizer"
    Environment = "production"
  }
}

# 2. Grupo de Segurança para o Banco de Dados RDS
resource "aws_security_group" "rds_sg" {
  name        = "sentinel-rds-sg"
  description = "Allow PostgreSQL access only from the backend EC2"

  # Porta 5432 acessível apenas pelo security group da EC2 — não pela internet
  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.sentinel_sg.id]
  }

  tags = {
    Name        = "sentinel-rds-sg"
    Project     = "sentinel-cloud-optimizer"
    Environment = "production"
  }
}

# 3. Instância EC2 (Onde o Java vai rodar)
resource "aws_instance" "sentinel_server" {
  ami           = data.aws_ami.ubuntu.id # AMI buscada dinamicamente acima
  instance_type = "t2.micro"
  key_name      = var.key_name

  vpc_security_group_ids = [aws_security_group.sentinel_sg.id]

  tags = {
    Name        = "sentinel-backend-server"
    Project     = "sentinel-cloud-optimizer"
    Environment = "production"
  }
}

# 4. Banco de Dados RDS (PostgreSQL)
resource "aws_db_instance" "sentinel_db" {
  allocated_storage    = 20
  db_name              = var.db_name
  engine               = "postgres"
  engine_version       = "16.3"
  instance_class       = "db.t4g.micro"
  username             = var.db_username
  password             = var.db_password
  parameter_group_name = "default.postgres16"
  skip_final_snapshot  = true

  # false por padrão — RDS não deve ser acessível pela internet em produção.
  # Acesso via security group da EC2 é suficiente e mais seguro.
  publicly_accessible = var.rds_publicly_accessible

  vpc_security_group_ids = [aws_security_group.rds_sg.id]

  tags = {
    Name        = "sentinel-rds"
    Project     = "sentinel-cloud-optimizer"
    Environment = "production"
  }
}
