# Sentinel Cloud Optimizer
Monitoramento inteligente de custos AWS com foco em eficiência e automação.

## Tecnologias Utilizadas
- **Infraestrutura**: AWS (EC2, RDS PostgreSQL), Terraform (IaC)
- **Backend**: Java 21, Spring Boot, Spring Data JPA, Hibernate
- **Coletor**: Python 3.x, Requests
- **Frontend**: HTML5, Tailwind CSS, Chart.js
- **DevOps**: GitHub Actions (CI/CD), Systemd (Linux Services)

## Arquitetura do Projeto
1. O **Terraform** provisiona toda a infraestrutura na AWS.
2. O **GitHub Actions** realiza o deploy automatizado do JAR para a EC2.
3. O **Python Collector** extrai métricas e as envia para a API REST.
4. O **Dashboard** consome a API e gera visualizações em tempo real.

## Funcionalidades
- [x] Coleta automatizada de custos via Python.
- [x] API REST para processamento de dados.
- [x] Sistema de alertas inteligentes para orçamentos excedidos.
- [x] Infraestrutura provisionada via código.

## Como Executar o Ecossistema

### 1. Infraestrutura (Terraform)
Provisione os recursos na AWS antes de realizar o deploy:
```bash
cd infrastructure
terraform init
terraform apply -auto-approve

Guarde o IP gerado e o endpoint do RDS para configurar as próximas etapas.

2. Backend (Java Spring Boot)
O deploy é automatizado via GitHub Actions. Para configurar manualmente:

Configure as variáveis de ambiente no GitHub Secrets (EC2_HOST, EC2_SSH_KEY, RDS_HOSTNAME, etc.).

Realize um git push para a branch main.

O sistema será instalado como um serviço Linux (Systemd) automaticamente.

Para gerenciar o serviço via SSH:

Bash

sudo systemctl status sentinel    # Verifica se está rodando
sudo journalctl -u sentinel -f    # Monitora os logs em tempo real
3. Coletor de Dados (Python)
Com o Backend online, execute o coletor para alimentar o banco:

Bash

cd worker-python
python collector.py
O script enviará os custos diretamente para o IP da sua instância EC2.

4. Frontend (Dashboard)
Acesse o Dashboard hospedado no GitHub Pages:
https://<seu-usuario>.github.io/sentinel-cloud-optimizer/frontend/index.html
(Lembre-se de permitir conteúdo inseguro/HTTP no navegador para visualizar os dados do gráfico).