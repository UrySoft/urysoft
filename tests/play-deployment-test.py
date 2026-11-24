import importlib.util
from pathlib import Path
import sys
import unittest
from unittest.mock import patch


module_path = Path(__file__).resolve().parents[1] / "docs/foldery-cobros.py"
spec = importlib.util.spec_from_file_location("play_setup", module_path)
setup = importlib.util.module_from_spec(spec)
sys.modules[spec.name] = setup
spec.loader.exec_module(setup)


class DeployTests(unittest.TestCase):
    path = "/accounts/" + "1" * 32 + "/pages/projects/urysoft"
    revision = "a" * 40

    def deployment(self, id, status="failure"):
        return {"id": id, "environment": "production", "latest_stage": {"name": "deploy", "status": status},
                "deployment_trigger": {"metadata": {"commit_hash": self.revision, "branch": "main"}}}

    def test_failed_tip_is_retried_once_and_availability_is_checked(self):
        failed = self.deployment("a1")
        newer = self.deployment("b2", "active")
        with patch.object(setup, "cloudflare", side_effect=[[failed], [failed], newer]) as api, \
             patch.object(setup, "current_source_commit", return_value=self.revision), \
             patch.object(setup, "billing_available", side_effect=[False, True]):
            setup.deploy_and_check("test", self.path)
        api.assert_called_with("test", self.path + "/deployments/a1/retry", method="POST")

    def test_newer_deployment_is_followed(self):
        with patch.object(setup, "cloudflare", side_effect=[[self.deployment("a1")], [self.deployment("b2", "active")]]) as api, \
             patch.object(setup, "billing_available", side_effect=[False, True]):
            setup.deploy_and_check("test", self.path)
        self.assertEqual(api.call_count, 2)

    def test_old_revision_and_cancelled_build_are_never_redeployed(self):
        for status in ["failure", "canceled"]:
            failed = self.deployment("a1", status)
            replies = [[failed], [failed], {"data": []}] if status == "failure" else [[failed], {"data": []}]
            with self.subTest(status=status), patch.object(setup, "cloudflare", side_effect=replies) as api, \
                 patch.object(setup, "current_source_commit", return_value="b" * 40), \
                 patch.object(setup, "billing_available", return_value=False), self.assertRaises(ValueError):
                setup.deploy_and_check("test", self.path)
            self.assertTrue(all(call.kwargs.get("method", "GET") == "GET" for call in api.call_args_list))

    def test_second_failure_reports_the_cause_without_log_contents(self):
        failed = self.deployment("a1")
        retry = self.deployment("b2")
        logs = {"data": [{"line": "too many bindings: PRIVATE_TEST_VALUE"}]}
        with patch.object(setup, "cloudflare", side_effect=[[failed], [failed], retry, logs]), \
             patch.object(setup, "current_source_commit", return_value=self.revision), \
             patch.object(setup, "billing_available", return_value=False), self.assertRaises(ValueError) as stopped:
            setup.deploy_and_check("test", self.path)
        self.assertIn("límite", str(stopped.exception))
        self.assertNotIn("PRIVATE_TEST_VALUE", str(stopped.exception))


if __name__ == "__main__":
    unittest.main()
