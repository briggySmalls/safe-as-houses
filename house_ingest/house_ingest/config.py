from pydantic import BaseModel, HttpUrl, AnyHttpUrl
import os
from dotenv import dotenv_values
from pathlib import Path


class Config(BaseModel):
	QUERY_URL: HttpUrl  # The URL to query for data
	ES_URL: AnyHttpUrl

	@classmethod
	def from_env(cls) -> "Config":
		env_vars = {
		    **dotenv_values(Path(os.getcwd()) / ".env"),  # Load variables from file (if present)
		    **os.environ,  # override loaded values with environment variables
		}
		return Config(**env_vars)
