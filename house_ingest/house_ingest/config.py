from pydantic import BaseModel
import os
from dotenv import dotenv_values
from pathlib import Path


class Config(BaseModel):
	QUERY_URL: str  # The URL to query for data

	@classmethod
	def from_env(cls) -> "Config":
		env_vars = {
		    **dotenv_values(Path(os.getcwd()) / ".env"),  # Load variables from file (if present)
		    **os.environ,  # override loaded values with environment variables
		}
		return Config(**env_vars)
