# SKU topic
resource "aws_sns_topic" "sku_notification" {
  name = "sku-notification-${var.env_name}"
}

# Product topic
resource "aws_sns_topic" "product_notification" {
  name = "product-notification-${var.env_name}"
}

# subscription to downloadEcomSku lambda
resource "aws_sns_topic_subscription" "sku_notification_lambda" {
  topic_arn = "${aws_sns_topic.sku_notification.arn}"
  protocol  = "lambda"
  endpoint  = "${module.lambda.downloadEcomSku_arn}"
}

# subscription to downloadEcomProduct lambda
resource "aws_sns_topic_subscription" "product_notification_lambda" {
  topic_arn = "${aws_sns_topic.product_notification.arn}"
  protocol  = "lambda"
  endpoint  = "${module.lambda.downloadEcomProduct_arn}"
}

# subscription  permission to downloadEcomSku lambda
resource "aws_lambda_permission" "downloadEcomSku_with_sns" {
  statement_id  = "AllowExecutionFromSNS"
  action        = "lambda:InvokeFunction"
  function_name = "${module.lambda.downloadEcomSku_function_name}"
  principal     = "sns.amazonaws.com"
  source_arn    = "${aws_sns_topic.sku_notification.arn}"
}

# subscription  permission to downloadEcomProduct lambda
resource "aws_lambda_permission" "downloadEcomProduct_with_sns" {
  statement_id  = "AllowExecutionFromSNS"
  action        = "lambda:InvokeFunction"
  function_name = "${module.lambda.downloadEcomProduct_function_name}"
  principal     = "sns.amazonaws.com"
  source_arn    = "${aws_sns_topic.product_notification.arn}"
}
