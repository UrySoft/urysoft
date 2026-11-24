"""Configurar la validación de suscripciones de una aplicación UrySoft mediante su cuenta de Google Play."""
import argparse
import json
import os
from pathlib import Path
from dataclasses import dataclass
import re
import shutil
import subprocess
import sys
import time
from urllib.error import HTTPError
from urllib.request import build_opener, HTTPRedirectHandler, Request, urlopen


SITE = "urysoft.pages.dev"


@dataclass(frozen=True)
class BillingApp:
    slug: str

    @property
    def name(self):
        return "TeclaIRC" if self.slug == "teclairc" else self.slug.capitalize()

    @property
    def product(self):
        return self.slug + "_premium" if self.slug in ("foldery", "widgery") else "premium"

    @property
    def service_secret(self):
        return self.slug.upper() + "_PLAY_SERVICE_ACCOUNT"

    @property
    def enabled_secret(self):
        return self.slug.upper() + "_PLAY_BILLING_ENABLED"

    @property
    def status_url(self):
        return f"https://{SITE}/api/play-billing/{self.slug}"


FOLDERY = BillingApp("foldery")
WRANGLER = ["npm", "exec", "--yes", "--package=node@22", "--package=wrangler@4.167.0", "--", "wrangler"]


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


def prepare_account(project, app=FOLDERY):
    scope = "--project=" + project
    account_id = app.slug + "-play-billing"
    expected = f"{account_id}@{project}.iam.gserviceaccount.com"
    gcloud("services", "enable", "iam.googleapis.com", "androidpublisher.googleapis.com", scope)
    existing = gcloud("iam", "service-accounts", "list", scope, "--format=value(email)")
    newly_created = expected not in existing.splitlines()
    if newly_created:
        gcloud("iam", "service-accounts", "create", account_id, scope, f"--display-name={app.name} - Google Play Billing")
    for attempt in range(8):
        described = subprocess.run(["gcloud", "iam", "service-accounts", "describe", expected, scope, "--format=json"],
                                   capture_output=True, text=True)
        if described.returncode == 0:
            break
        if not newly_created or "NOT_FOUND" not in described.stderr or attempt == 7:
            raise ValueError(f"Google no ha podido consultar {expected}. {described.stderr.strip()}")
        time.sleep(3)
    account = json.loads(described.stdout)
    if account.get("disabled") or account.get("email") != expected:
        raise ValueError("Google no ha confirmado que la cuenta de servicio esté activa.")
    print("Envía a quien gestiona Play Console únicamente este correo:")
    print(account["email"])


def read_credential(project, filename):
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
    return {field: credential[field] for field in ("type", "client_email", "private_key_id", "private_key")}


def connect_credential(project, filename, app=FOLDERY):
    if not shutil.which("wrangler"):
        raise ValueError("Se necesita Wrangler, la herramienta oficial de Cloudflare, con acceso al proyecto urysoft.")
    signing = read_credential(project, filename)
    owner = signing["client_email"]
    if app.slug != "foldery" and owner != f"{app.slug}-play-billing@{project}.iam.gserviceaccount.com":
        raise ValueError(f"{app.name} requiere su propia cuenta de servicio.")
    projects = subprocess.run(["wrangler", "pages", "project", "list"], check=True, capture_output=True, text=True)
    if not re.search(r"(?<![a-z0-9.-])urysoft\.pages\.dev(?![a-z0-9.-])", projects.stdout, re.I):
        raise ValueError("La sesión de Cloudflare no muestra el proyecto que sirve urysoft.pages.dev.")
    print(f"Cuenta de Google: {owner}. Destino: proyecto urysoft de Cloudflare Pages, entorno de producción.")
    print(f"Comprueba en Play Console que esta cuenta ya tiene los permisos de {app.name} para consultar compras y gestionar suscripciones.")
    confirmation = "ACTIVAR " + app.slug.upper()
    if input(f"Para guardar su credencial y activar la facturación, escribe {confirmation}: ").strip() != confirmation:
        raise ValueError("Se ha cancelado la activación.")
    for name, value in ((app.service_secret, json.dumps(signing)), (app.enabled_secret, "yes")):
        result = subprocess.run(
            ["wrangler", "pages", "secret", "put", name, "--project-name", "urysoft", "--env", "production"],
            input=value, text=True, capture_output=True)
        if result.returncode:
            raise ValueError(f"Cloudflare no ha confirmado el ajuste {name}. Puedes reintentar con la misma credencial.")
    print("Cloudflare ha guardado ambos ajustes. Vuelve a desplegar la versión de producción para aplicarlos.")
    print(f"Después, comprueba {app.status_url} y verifica una compra con un usuario de pruebas de Play.")


def billing_available(app=FOLDERY):
    request = Request(app.status_url,
                      headers={"Cache-Control": "no-cache", "User-Agent": "UrySoft-Play/1.0"})
    with urlopen(request, timeout=15) as response:
        status = json.load(response)
    if status.get("product") != app.product or status.get("plan") != "annual":
        raise ValueError(f"El servidor no ha identificado el plan anual de {app.name}.")
    return status.get("available") is True


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, *args):
        return None


def cloudflare(token, path, method="GET", find_project=False):
    request = Request("https://api.cloudflare.com/client/v4" + path, method=method,
                      headers={"Authorization": "Bearer " + token, "User-Agent": "UrySoft-Play/1.0"})
    try:
        with build_opener(NoRedirect).open(request, timeout=30) as response:
            data = json.load(response)
    except HTTPError as error:
        if find_project and error.code in (403, 404):
            return None
        raise ValueError(f"Cloudflare ha devuelto {error.code} al consultar o desplegar el proyecto.") from None
    if not data.get("success"):
        raise ValueError("Cloudflare no ha confirmado la operación.")
    return data["result"]


def pages_session(wrangler):
    command = [*wrangler, "whoami", "--json"]
    session = subprocess.run(command, capture_output=True, text=True)
    if session.returncode:
        print("Autoriza en Cloudflare el código que muestra Wrangler para la cuenta de UrySoft.", flush=True)
        subprocess.run([*wrangler, "login", "--device", "--browser=false", "--scopes",
                        "account:read", "user:read", "pages:write"], check=True)
        session = subprocess.run(command, check=True, capture_output=True, text=True)
    identity = json.loads(session.stdout)
    # El token solo se usa en memoria con api.cloudflare.com, nunca en argumentos ni en la salida.
    result = subprocess.run([*wrangler, "auth", "token", "--json"], check=True, capture_output=True, text=True)
    token = json.loads(result.stdout).get("token")
    if not token:
        raise ValueError("Autoriza Wrangler con una sesión de Cloudflare para UrySoft.")
    for account in identity.get("accounts", []):
        account_id = account.get("id", "")
        if re.fullmatch(r"[a-f0-9]{32}", account_id):
            path = f"/accounts/{account_id}/pages/projects/urysoft"
            project = cloudflare(token, path, find_project=True)
            if project and project.get("name") == "urysoft" and project.get("subdomain") == SITE:
                return token, account_id, path, project
    raise ValueError("Esta sesión de Cloudflare no tiene acceso al proyecto urysoft.pages.dev.")


def install_secrets(project, account_id, wrangler, keyfile, app=FOLDERY):
    values = {app.enabled_secret: "yes"}
    if keyfile is not None:
        owner = f"{app.slug}-play-billing@{project}.iam.gserviceaccount.com"
        if not keyfile.exists():
            gcloud("iam", "service-accounts", "keys", "create", str(keyfile),
                   "--iam-account=" + owner, "--project=" + project)
        signing = read_credential(project, keyfile)
        if signing["client_email"] != owner:
            raise ValueError(f"La credencial local no corresponde a la cuenta de servicio de {app.name}.")
        values[app.service_secret] = json.dumps(signing)
    result = subprocess.run(
        [*wrangler, "pages", "secret", "bulk", "--project-name", "urysoft", "--env", "production"],
        input=json.dumps(values), text=True,
        capture_output=True, env={**os.environ, "CLOUDFLARE_ACCOUNT_ID": account_id})
    if result.returncode:
        raise ValueError("Cloudflare no ha guardado los dos secretos. Repite el comando para reutilizar la misma clave.")


def has_secret(project, name):
    variables = project["deployment_configs"]["production"].get("env_vars") or {}
    return (variables.get(name) or {}).get("type") == "secret_text"


def current_source_commit():
    request = Request("https://api.github.com/repos/UrySoft/urysoft/git/ref/heads/main",
                      headers={"User-Agent": "UrySoft-Play/1.0", "Cache-Control": "no-cache"})
    with urlopen(request, timeout=15) as response:
        return json.load(response)["object"]["sha"]


def deployment_failure(token, path, deployment):
    stage = deployment.get("latest_stage") or {}
    known_stages = {"queued", "initialize", "clone_repo", "build", "deploy"}
    name = stage.get("name")
    deployment_id = deployment["id"]
    message = f"Cloudflare ha detenido el despliegue {deployment_id}"
    if name in known_stages:
        message += f" en la fase {name}"
    # Los registros completos pueden contener datos de la cuenta. Solo se muestran causas conocidas.
    try:
        log = cloudflare(token, path + "/deployments/" + deployment_id + "/history/logs")
        lines = " ".join(str(entry.get("line", "")) for entry in log.get("data", [])).lower()
        errors = (
            (r"too many (?:environment variables|bindings)|exceed.{0,50}(?:binding|variable) limit|maximum.{0,30}(?:environment variables|bindings)", "límite de variables o bindings"),
            (r"file.{0,60}(?:exceeds|over|larger than).{0,30}(?:mib|mb)|too many files", "límite de archivos"),
            (r"exceeded.{0,70}(?:cpu|memory)|out of memory|heap limit", "límite de CPU o memoria"),
            (r"could not resolve|module not found|cannot find module", "dependencia no encontrada"),
            (r"failed to clone|failed to fetch.{0,40}(?:repo|git)", "descarga del repositorio"),
            (r"internal (?:server )?error|internal platform error", "error interno de Cloudflare"),
        )
        for pattern, description in errors:
            if re.search(pattern, lines):
                message += f" ({description})"
                break
    except (OSError, TypeError, ValueError):
        pass
    account = path.split("/")[2]
    return (message + ". Los secretos se han conservado. Registro: "
            f"https://dash.cloudflare.com/{account}/pages/view/urysoft/{deployment_id}")


def deploy_and_check(token, path, app=FOLDERY):
    # Reutiliza una compilación en curso y nunca vuelve a publicar una revisión anterior.
    latest = cloudflare(token, path + "/deployments?env=production&per_page=1")
    if not latest or latest[0].get("environment") != "production":
        raise ValueError("No se ha encontrado un despliegue de producción de UrySoft.")
    deployment = latest[0]
    restarted = False
    print("Cloudflare está aplicando la configuración de producción.", flush=True)
    for _ in range(40):
        try:
            available = billing_available(app)
        except (OSError, ValueError):
            available = False
        if available:
            print(f"El servidor de {app.name} ya admite la validación de compras del plan anual.")
            return
        stage = deployment.get("latest_stage") or {}
        deployment_id = deployment.get("id", "")
        if not re.fullmatch(r"[a-fA-F0-9-]+", deployment_id):
            raise ValueError("Cloudflare no ha confirmado el identificador del despliegue.")
        failed = stage.get("status") == "failure"
        finished = stage.get("name") == "deploy" and stage.get("status") == "success"
        if stage.get("status") in ("canceled", "cancelled"):
            raise ValueError(deployment_failure(token, path, deployment))
        if failed and restarted:
            raise ValueError(deployment_failure(token, path, deployment))
        if not restarted and (failed or finished):
            latest = cloudflare(token, path + "/deployments?env=production&per_page=1")
            if not latest or latest[0].get("environment") != "production":
                raise ValueError("Cloudflare no ha confirmado el último despliegue de producción.")
            if latest[0].get("id") != deployment_id:
                deployment = latest[0]
                continue
            if failed:
                metadata = (deployment.get("deployment_trigger") or {}).get("metadata") or {}
                revision = metadata.get("commit_hash", "")
                if metadata.get("branch") != "main" or metadata.get("commit_dirty") or not re.fullmatch(r"[0-9a-f]{40}", revision):
                    raise ValueError(deployment_failure(token, path, deployment))
                if revision != current_source_commit():
                    raise ValueError(deployment_failure(token, path, deployment) + " La rama main ya tiene otra revisión.")
            else:
                canonical = cloudflare(token, path).get("canonical_deployment") or {}
                if canonical.get("id") != deployment_id:
                    raise ValueError("Cloudflare aún no ha confirmado la revisión publicada. Repite la activación.")
            deployment = cloudflare(token, path + "/deployments/" + deployment_id + "/retry", method="POST")
            restarted = True
            continue
        time.sleep(8)
        deployment = cloudflare(token, path + "/deployments/" + deployment_id)
    raise ValueError(f"Los secretos están guardados. Comprueba el despliegue y {app.status_url}.")


def activate(project, app=FOLDERY):
    import fcntl  # Cloud Shell ejecuta Linux.
    if billing_available(app):
        print(f"El servidor de {app.name} ya admite la validación de compras del plan anual.")
        return
    if not shutil.which("npm"):
        raise ValueError("El paso activar necesita npm en Google Cloud Shell.")
    # El paquete de Node permite utilizar Wrangler también con la versión anterior de Node de Cloud Shell.
    wrangler = WRANGLER
    os.umask(0o077)
    directory = Path.home() / ".config" / "urysoft" / (app.slug + "-play") / project
    directory.mkdir(mode=0o700, parents=True, exist_ok=True)
    with (directory / "activar.lock").open("a") as lock:
        try:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            raise ValueError(f"Ya hay una activación de {app.name} en curso en esta terminal de Cloud Shell.") from None
        token, account_id, path, pages = pages_session(wrangler)
        keyfile = None if has_secret(pages, app.service_secret) else directory / "credencial-pendiente.json"
        install_secrets(project, account_id, wrangler, keyfile, app)
        confirmed = cloudflare(token, path)
        if not all(has_secret(confirmed, name) for name in (app.service_secret, app.enabled_secret)):
            raise ValueError("Cloudflare aún no confirma los dos secretos. Puedes repetir el comando.")
        if keyfile is not None:
            keyfile.unlink()
        print(f"Cloudflare confirma los dos ajustes cifrados de {app.name} en producción.", flush=True)
        deploy_and_check(token, path, app)


def diagnose(app):
    token, _, path, project = pages_session(WRANGLER)
    secrets_present = all(has_secret(project, name) for name in (app.service_secret, app.enabled_secret))
    print(f"{app.name}: ajustes cifrados {'confirmados' if secrets_present else 'incompletos'}.")
    print(f"Validación del plan anual: {'disponible' if billing_available(app) else 'pendiente'}.")
    production = cloudflare(token, path + "/deployments?env=production&per_page=1")
    if production and production[0].get("environment") == "production":
        deployment = production[0]
        stage = deployment.get("latest_stage") or {}
        if stage.get("status") in ("failure", "canceled", "cancelled") and re.fullmatch(r"[a-fA-F0-9-]+", deployment.get("id", "")):
            print(deployment_failure(token, path, deployment))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("paso", nargs="?", choices=["cuenta", "conectar", "activar", "diagnostico"], default="cuenta",
                        help="cuenta prepara la identidad, conectar importa una credencial, activar configura y despliega, diagnostico consulta el estado")
    parser.add_argument("--app", choices=["foldery", "widgery", "storagey", "teclairc"], default="foldery", help="aplicación que se va a configurar")
    parser.add_argument("--proyecto", help="identificador del proyecto de Google Cloud de UrySoft")
    parser.add_argument("--credencial", type=Path, help="archivo local de la cuenta autorizada, solo para conectar")
    args = parser.parse_args()
    if bool(args.credencial) != (args.paso == "conectar"):
        parser.error("El paso conectar necesita --credencial. El paso cuenta no utiliza claves.")
    app = BillingApp(args.app)
    if args.paso == "diagnostico":
        diagnose(app)
        return
    if not shutil.which("gcloud"):
        parser.error("Ejecuta el programa en Google Cloud Shell o en un equipo con gcloud y tu sesión de UrySoft.")
    project = select_project(args.proyecto)
    if args.paso == "cuenta":
        prepare_account(project, app)
    elif args.paso == "conectar":
        connect_credential(project, args.credencial, app)
    else:
        activate(project, app)


if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError, EOFError, subprocess.CalledProcessError) as error:
        sys.exit(str(error))
