provider "aws" {
  version = "~> 1.54"
}

# Retrieve information about current region
data "aws_region" "current" {}

module "lambda" {
  source = "lambda"

  env_name    = "${lower(var.env_name)}"
  code_bucket = "${var.code_bucket}"
  jar_path    = "${var.jar_path}"
  output_bucket      = "${var.output_bucket}"
  product_table_name = "${aws_dynamodb_table.ecom_product.name}"
  sku_table_name     = "${aws_dynamodb_table.ecom_sku.name}"

  required_tags = "${local.required_tags}"
}

module "api_gateway" {
  source = "api_gateway"

  env_name   = "${lower(var.env_name)}"
  env_region = "${data.aws_region.current.name}"

  getEcomSku_arn = "${module.lambda.getEcomSku_arn}"
  putEcomSku_arn = "${module.lambda.putEcomSku_arn}"
  getEcomSku_invoke_arn = "${module.lambda.getEcomSku_invoke_arn}"
  putEcomSku_invoke_arn = "${module.lambda.putEcomSku_invoke_arn}"

  getEcomProduct_arn = "${module.lambda.getEcomProduct_arn}"
  putEcomProduct_arn = "${module.lambda.putEcomProduct_arn}"
  getEcomProduct_invoke_arn = "${module.lambda.getEcomProduct_invoke_arn}"
  putEcomProduct_invoke_arn = "${module.lambda.putEcomProduct_invoke_arn}"

  downloadEcomProduct_arn = "${module.lambda.downloadEcomProduct_arn}"
  downloadEcomSku_arn     = "${module.lambda.downloadEcomSku_arn}"
  downloadEcomProduct_invoke_arn = "${module.lambda.downloadEcomProduct_invoke_arn}"
  downloadEcomSku_invoke_arn     = "${module.lambda.downloadEcomSku_invoke_arn}"

  product_notification_topic_arn = "${aws_sns_topic.product_notification.arn}"
  sku_notification_topic_arn     = "${aws_sns_topic.sku_notification.arn}"

  required_tags = "${local.required_tags}"
}
