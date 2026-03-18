import requests
import datetime

# Configuração: Onde o seu Java está rodando
API_URL = "http://100.53.185.3:8080/api/costs"

def enviar_custo(nome_recurso, valor):
    dados = {
        "resourceName": nome_recurso,
        "costAmount": valor,
        "currency": "USD"
    }
    
    try:
        response = requests.post(API_URL, json=dados)
        if response.status_code == 200 or response.status_code == 201:
            print(f"✅ Sucesso: {nome_recurso} enviado!")
        else:
            print(f"❌ Erro ao enviar: {response.status_code}")
    except Exception as e:
        print(f"⚠️ O Backend Java está desligado? Erro: {e}")

if __name__ == "__main__":
    print(" Iniciando Coletor Sentinel...")
    enviar_custo("Instância EC2 - Produção", 45.50)
    enviar_custo("Banco RDS - QA", 12.75)
    