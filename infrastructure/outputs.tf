# ============================================================
# Outputs — valores exibidos após terraform apply
# ============================================================

output "ec2_public_ip" {
  description = "IP público da instância EC2. Use para configurar o collector e o frontend."
  value       = aws_instance.sentinel_server.public_ip
}

output "rds_endpoint" {
  description = "Endpoint do RDS. Use como DATABASE_URL no application-prod.properties."
  value       = aws_db_instance.sentinel_db.endpoint
  sensitive   = false
}
