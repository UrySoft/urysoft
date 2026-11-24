"""Preparar la cuenta de Google Play de Foldery y conectar una credencial al servidor."""
import argparse
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys


def gcloud(*arguments):
    result = subprocess.run(["gcloud", *arguments], check=True, stdout=subprocess.PIPE, text=True)
    return result.stdout.strip()


def select_project(chosen):
    if chosen:
        project = chosen
    else:
        project = gcloud("config", "get-value", "project")
        if project in ("", "(unset)"):
            raise ValueError("Selecciona el proyecto de UrySoft en Google Cloud o usa --proyecto.")
        print(f"Proyecto activo en Google Cloud: {project}.")
        if input("Para usarlo, escribe su identificador: ").strip() != project:
            raise ValueError("Se ha cancelado la configuración.")
    if not re.fullmatch(r"[a-z][a-z0-9-]{4,28}[a-z0-9]", project):
        raise ValueError("Identificador de proyecto no válido.")
    return project


def prepare_account(project):
    scope = "--project=" + project
    account_id = "foldery-play-billing"
    expected = f"{account_id}@{project}.iam.gserviceaccount.com"
    gcloud("services", "enable", "iam.googleapis.com", "androidpublisher.googleapis.com", scope)
    existing = gcloud("iam", "service-accounts", "list", scope, "--filter=email=" + expected, "--format=value(email)")
    if expected not in existing.splitlines():
        gcloud("iam", "service-accounts", "create", account_id, scope, "--display-name=Foldery - Google Play Billing")
    account = json.loads(gcloud("iam", "service-accounts", "describe", expected, scope, "--format=json"))
    if account.get("disabled") or account.get("email") != expected:
        raise ValueError("Google no ha confirmado que la cuenta de servicio esté activa.")
    print("Envía a quien gestiona Play Console únicamente este correo:")
    print(account["email"])


def connect_credential(project, filename):
    if not shutil.which("wrangler"):
        raise ValueError("Se necesita Wrangler, la herramienta oficial de Cloudflare, con acceso al proyecto urysoft.")
    credential = json.loads(filename.read_text(encoding="utf-8"))
    if not isinstance(credential, dict):
        raise ValueError("La credencial de Google debe ser un objeto.")
    owner = credential.get("client_email", "")
    if (credential.get("type") != "service_account" or credential.get("project_id") != project
            or not re.fullmatch(r"[a-z0-9][a-z0-9-]*@" + re.escape(project) + r"\.iam\.gserviceaccount\.com", owner)
            or not credential.get("private_key_id") or not credential.get("private_key", "").startswith("-----BEGIN PRIVATE KEY-----")):
        raise ValueError("El archivo debe ser la credencial de una cuenta de servicio del proyecto elegido.")
    account = json.loads(gcloud("iam", "service-accounts", "describe", owner, "--project=" + project, "--format=json"))
    if account.get("disabled") or account.get("email") != owner or (credential.get("client_id") and credential["client_id"] != account.get("uniqueId")):
        raise ValueError("Google no ha confirmado la identidad de la cuenta.")
    projects = subprocess.run(["wrangler", "pages", "project", "list"], check=True, capture_output=True, text=True)
    if not re.search(r"(?<![a-z0-9.-])urysoft\.pages\.dev(?![a-z0-9.-])", projects.stdout, re.I):
        raise ValueError("La sesión de Cloudflare no muestra el proyecto que sirve urysoft.pages.dev.")
    print(f"Cuenta de Google: {owner}. Destino: proyecto urysoft de Cloudflare Pages, entorno de producción.")
    print("Comprueba en Play Console que esta cuenta ya tiene los permisos de Foldery para consultar compras y gestionar suscripciones.")
    if input("Para guardar su credencial cifrada y activar la facturación, escribe ACTIVAR FOLDERY: ").strip() != "ACTIVAR FOLDERY":
        raise ValueError("Se ha cancelado la activación.")
    signing = {field: credential[field] for field in ("type", "client_email", "private_key_id", "private_key")}
    for name, value in (("FOLDERY_PLAY_SERVICE_ACCOUNT", json.dumps(signing)), ("FOLDERY_PLAY_BILLING_ENABLED", "yes")):
        result = subprocess.run(
            ["wrangler", "pages", "secret", "put", name, "--project-name", "urysoft", "--env", "production"],
            input=value, text=True, capture_output=True)
        if result.returncode:
            raise ValueError(f"Cloudflare no ha confirmado el ajuste {name}. Puedes reintentar con la misma credencial.")
    print("Cloudflare ha guardado ambos ajustes. Vuelve a desplegar la versión de producción para aplicarlos.")
    print("Después, comprueba https://urysoft.pages.dev/api/play-billing/foldery y verifica una compra con un usuario de pruebas de Play.")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("paso", nargs="?", choices=["cuenta", "conectar"], default="cuenta",
                        help="cuenta crea o recupera la identidad técnica sin generar claves")
    parser.add_argument("--proyecto", help="identificador del proyecto de Google Cloud de UrySoft")
    parser.add_argument("--credencial", type=Path, help="archivo local de la cuenta autorizada, solo para conectar")
    args = parser.parse_args()
    if bool(args.credencial) != (args.paso == "conectar"):
        parser.error("El paso conectar necesita --credencial. El paso cuenta no utiliza claves.")
    if not shutil.which("gcloud"):
        parser.error("Ejecuta el programa en Google Cloud Shell o en un equipo con gcloud y tu sesión de UrySoft.")
    project = select_project(args.proyecto)
    if args.paso == "cuenta":
        prepare_account(project)
    else:
        connect_credential(project, args.credencial)


if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError, EOFError, subprocess.CalledProcessError) as error:
        sys.exit(str(error))
