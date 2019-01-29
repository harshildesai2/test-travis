variable "env_name" {
  description = "Environment name [dev, qa, stage, prod]"
}

variable "env_tag" {
  description = "Environment name for tagging [dev, qa, stg, prod]"
  default = ""
}

variable "code_bucket" {
  description = "S3 bucket name for code"
  default = "lll-ecom-catalog"
}

variable "jar_path" {
  description = "Path of code JAR in S3 bucket"
  default = "code/catalog-1.0.0.jar"
}

variable "output_bucket" {
  description = "S3 bucket name for code"
  default = "lll-ecom-catalog"
}

variable "department" {
  description = "Department tag"
}

variable "project_name" {
  description = "Project name tag"
}

variable "project_code" {
  description = "Project code tag"
}

variable "cost_center" {
  description = "Cost center tag"
}

locals {
  required_tags = {
    "lll:deployment:terraform"      = "True"
    "lll:business:application-name" = "ECom Catalog"
    "lll:deployment:environment"    = "${var.env_tag == "" ? lower(var.env_name) : lower(var.env_tag)}"
    "lll:business:department"       = "${var.department}"
    "lll:business:project-name"     = "${var.project_name}"
    "lll:business:project-code"     = "${var.project_code}"
    "lll:business:cost-centre"      = "${var.cost_center}"
  }
}
