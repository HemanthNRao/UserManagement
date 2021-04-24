package org.RAO.userManagement.exceptions

class MissingParams(missingParams: List[String]) extends Exception(s"Following parameters are missing ${missingParams.mkString(", ")}")
