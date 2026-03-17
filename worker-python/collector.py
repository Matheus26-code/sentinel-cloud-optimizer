"""
Sentinel Cloud Optimizer — Cost Collector

Simula a coleta de custos de recursos AWS e os envia para a API REST.
Em produção, este script seria substituído por chamadas reais à AWS Cost Explorer API.

Uso:
    python collector.py                        # usa defaults
    python collector.py --api-url http://...   # URL customizada
    python collector.py --dry-run              # simula sem enviar
    python collector.py --retries 5 --delay 2  # configura retry
"""

import argparse
import logging
import os
import random
import sys
import time

import requests

# =============================================================
# Logging — substitui print() em código de produção.
# Benefícios: níveis (DEBUG/INFO/WARNING/ERROR), timestamps,
# redirecionamento para arquivo, integração com ferramentas de APM.
# =============================================================
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
log = logging.getLogger(__name__)


# =============================================================
# Dados simulados — representam recursos AWS realistas.
# Em produção: substituir por boto3 + AWS Cost Explorer API.
# =============================================================
SIMULATED_RESOURCES = [
    {"resourceName": "EC2 - t3.medium (prod)",      "baseCost": 30.37},
    {"resourceName": "EC2 - t2.micro (staging)",    "baseCost": 8.35},
    {"resourceName": "RDS - db.t4g.micro (prod)",   "baseCost": 14.64},
    {"resourceName": "RDS - db.t3.medium (qa)",     "baseCost": 52.56},
    {"resourceName": "S3 - logs-bucket",            "baseCost": 2.30},
    {"resourceName": "S3 - backups-bucket",         "baseCost": 5.10},
    {"resourceName": "CloudWatch - prod",           "baseCost": 3.75},
    {"resourceName": "Lambda - data-processor",     "baseCost": 0.85},
    {"resourceName": "ElastiCache - redis (prod)",  "baseCost": 24.80},
    {"resourceName": "NAT Gateway",                 "baseCost": 38.40},
]


def build_args() -> argparse.Namespace:
    """Configura e faz parse dos argumentos de linha de comando."""
    parser = argparse.ArgumentParser(
        description="Sentinel Cost Collector — envia custos AWS para a API REST"
    )
    parser.add_argument(
        "--api-url",
        default=os.environ.get("API_URL", "http://localhost:8080/api/costs"),
        help="URL da API REST (default: $API_URL ou http://localhost:8080/api/costs)",
    )
    parser.add_argument(
        "--retries",
        type=int,
        default=int(os.environ.get("COLLECTOR_RETRIES", "3")),
        help="Número de tentativas em caso de falha (default: 3)",
    )
    parser.add_argument(
        "--delay",
        type=float,
        default=float(os.environ.get("COLLECTOR_RETRY_DELAY", "1.0")),
        help="Segundos entre tentativas (default: 1.0)",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Simula a execução sem enviar dados para a API",
    )
    return parser.parse_args()


def generate_cost(base: float) -> float:
    """
    Aplica variação aleatória de ±15% ao custo base.
    Simula flutuação real de custos AWS ao longo do mês.
    """
    variation = random.uniform(-0.15, 0.15)
    return round(base * (1 + variation), 2)


def send_cost(api_url: str, resource_name: str, cost_amount: float,
              retries: int, delay: float) -> bool:
    """
    Envia um custo para a API com retry exponencial.

    Retry com backoff: a cada falha, dobra o tempo de espera.
    Evita sobrecarregar um servidor que já está com problemas.

    Returns:
        True se enviado com sucesso, False após esgotar as tentativas.
    """
    payload = {
        "resourceName": resource_name,
        "costAmount": cost_amount,
    }

    for attempt in range(1, retries + 1):
        try:
            response = requests.post(api_url, json=payload, timeout=10)
            response.raise_for_status()  # lança exceção para 4xx/5xx
            log.info("Sent: %-40s $%.2f", resource_name, cost_amount)
            return True

        except requests.exceptions.ConnectionError:
            log.warning("Attempt %d/%d — connection refused. Is the backend running?",
                        attempt, retries)
        except requests.exceptions.Timeout:
            log.warning("Attempt %d/%d — request timed out.", attempt, retries)
        except requests.exceptions.HTTPError as e:
            log.error("Attempt %d/%d — HTTP error: %s", attempt, retries, e)
            # Erros 4xx (ex: 400 Bad Request) não devem ser retentados
            if response.status_code < 500:
                return False

        if attempt < retries:
            wait = delay * (2 ** (attempt - 1))  # backoff exponencial: 1s, 2s, 4s...
            log.info("Retrying in %.1fs...", wait)
            time.sleep(wait)

    log.error("Failed to send '%s' after %d attempts.", resource_name, retries)
    return False


def run(args: argparse.Namespace) -> int:
    """
    Executa o ciclo de coleta e envio.

    Returns:
        Exit code: 0 = sucesso total, 1 = falhas parciais ou totais.
    """
    log.info("Starting Sentinel Cost Collector")
    log.info("API URL : %s", args.api_url)
    log.info("Dry run : %s", args.dry_run)

    successes = 0
    failures = 0

    for resource in SIMULATED_RESOURCES:
        cost = generate_cost(resource["baseCost"])

        if args.dry_run:
            log.info("[DRY RUN] Would send: %-40s $%.2f", resource["resourceName"], cost)
            successes += 1
            continue

        ok = send_cost(
            api_url=args.api_url,
            resource_name=resource["resourceName"],
            cost_amount=cost,
            retries=args.retries,
            delay=args.delay,
        )
        if ok:
            successes += 1
        else:
            failures += 1

    log.info("Done. %d sent, %d failed.", successes, failures)
    return 0 if failures == 0 else 1


if __name__ == "__main__":
    args = build_args()
    sys.exit(run(args))
