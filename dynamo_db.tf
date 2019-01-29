#dynamo db table for product
resource "aws_dynamodb_table" "ecom_product" {
  name = "ecom-product-${var.env_name}"
  billing_mode = "PAY_PER_REQUEST"
  hash_key = "id"

  attribute {
    name = "id"
    type = "S"
  }

  tags = "${local.required_tags}"
}

#dynamo db table for sku
resource "aws_dynamodb_table" "ecom_sku" {
  name = "ecom-sku-${var.env_name}"
  billing_mode = "PAY_PER_REQUEST"
  hash_key = "id"

  attribute {
    name = "id"
    type = "S"
  }

  tags = "${local.required_tags}"
}
