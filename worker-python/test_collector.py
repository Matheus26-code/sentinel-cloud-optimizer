"""
Testes unitários do collector.py usando pytest + pytest-mock.

Executar:
    pip install -r requirements-dev.txt
    pytest test_collector.py -v
"""

import argparse

import pytest
import requests

from collector import build_args, generate_cost, run, send_cost


# =============================================================
# Helpers
# =============================================================

def make_args(**kwargs) -> argparse.Namespace:
    """Cria um Namespace com valores default para os testes."""
    defaults = {
        "api_url": "http://localhost:8080/api/costs",
        "retries": 3,
        "delay": 0.0,   # sem espera nos testes
        "dry_run": False,
    }
    defaults.update(kwargs)
    return argparse.Namespace(**defaults)


def make_ok_response(mocker, status=201):
    """Cria um mock de resposta HTTP de sucesso."""
    mock_response = mocker.Mock()
    mock_response.status_code = status
    mock_response.raise_for_status = mocker.Mock()  # não lança exceção
    return mock_response


# =============================================================
# generate_cost
# =============================================================

def test_generate_cost_stays_within_15_percent_variation():
    base = 100.0
    for _ in range(200):  # múltiplas iterações para cobrir aleatoriedade
        result = generate_cost(base)
        assert 85.0 <= result <= 115.0, f"Custo fora da faixa esperada: {result}"


def test_generate_cost_returns_two_decimal_places():
    result = generate_cost(30.37)
    assert result == round(result, 2)


# =============================================================
# send_cost — sucesso
# =============================================================

def test_send_cost_returns_true_on_201(mocker):
    mocker.patch("collector.requests.post", return_value=make_ok_response(mocker, 201))

    result = send_cost(
        api_url="http://api/costs",
        resource_name="EC2-test",
        cost_amount=25.0,
        retries=3,
        delay=0.0,
    )

    assert result is True


def test_send_cost_returns_true_on_200(mocker):
    mocker.patch("collector.requests.post", return_value=make_ok_response(mocker, 200))

    result = send_cost(
        api_url="http://api/costs",
        resource_name="S3-test",
        cost_amount=5.0,
        retries=1,
        delay=0.0,
    )

    assert result is True


# =============================================================
# send_cost — falhas e retry
# =============================================================

def test_send_cost_retries_on_connection_error(mocker):
    """Deve tentar N vezes antes de desistir em ConnectionError."""
    mock_post = mocker.patch(
        "collector.requests.post",
        side_effect=requests.exceptions.ConnectionError("refused"),
    )
    mock_sleep = mocker.patch("collector.time.sleep")

    result = send_cost(
        api_url="http://api/costs",
        resource_name="EC2",
        cost_amount=10.0,
        retries=3,
        delay=1.0,
    )

    assert result is False
    assert mock_post.call_count == 3
    # backoff: sleep chamado entre as tentativas (não após a última)
    assert mock_sleep.call_count == 2


def test_send_cost_retries_on_timeout(mocker):
    mock_post = mocker.patch(
        "collector.requests.post",
        side_effect=requests.exceptions.Timeout("timeout"),
    )
    mocker.patch("collector.time.sleep")

    result = send_cost(
        api_url="http://api/costs",
        resource_name="RDS",
        cost_amount=50.0,
        retries=2,
        delay=0.0,
    )

    assert result is False
    assert mock_post.call_count == 2


def test_send_cost_does_not_retry_on_400(mocker):
    """Erros 4xx são definitivos — não deve retentar."""
    mock_response = mocker.Mock()
    mock_response.status_code = 400
    http_error = requests.exceptions.HTTPError(response=mock_response)
    mock_response.raise_for_status.side_effect = http_error

    mock_post = mocker.patch("collector.requests.post", return_value=mock_response)

    result = send_cost(
        api_url="http://api/costs",
        resource_name="BadRequest",
        cost_amount=10.0,
        retries=3,
        delay=0.0,
    )

    assert result is False
    assert mock_post.call_count == 1  # não retentou


def test_send_cost_succeeds_on_second_attempt(mocker):
    """Simula falha na 1ª tentativa e sucesso na 2ª."""
    mocker.patch(
        "collector.requests.post",
        side_effect=[
            requests.exceptions.ConnectionError("first attempt fails"),
            make_ok_response(mocker, 201),
        ],
    )
    mocker.patch("collector.time.sleep")

    result = send_cost(
        api_url="http://api/costs",
        resource_name="EC2",
        cost_amount=20.0,
        retries=3,
        delay=1.0,
    )

    assert result is True


# =============================================================
# run — dry-run
# =============================================================

def test_run_dry_run_does_not_call_api(mocker):
    """--dry-run não deve fazer nenhuma requisição HTTP."""
    mock_post = mocker.patch("collector.requests.post")

    exit_code = run(make_args(dry_run=True))

    assert exit_code == 0
    mock_post.assert_not_called()


# =============================================================
# run — comportamento geral
# =============================================================

def test_run_returns_zero_when_all_succeed(mocker):
    mocker.patch("collector.requests.post", return_value=make_ok_response(mocker, 201))

    exit_code = run(make_args())

    assert exit_code == 0


def test_run_returns_one_when_all_fail(mocker):
    mocker.patch(
        "collector.requests.post",
        side_effect=requests.exceptions.ConnectionError("backend down"),
    )
    mocker.patch("collector.time.sleep")

    exit_code = run(make_args(retries=1, delay=0.0))

    assert exit_code == 1
