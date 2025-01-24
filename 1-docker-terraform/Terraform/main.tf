terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "6.16.0"
    }
  }
}

provider "google" {
  credentials = file(var.gcp_cred)
  project     = var.gcp_project
  region      = var.gcp_region
}


resource "google_storage_bucket" "demo-bucket" {
  name          = var.gcp_bucket_name
  location      = var.gcp_location
  force_destroy = true
  storage_class = var.gcp_storage_class

  lifecycle_rule {
    condition {
      age = 1
    }
    action {
      type = "AbortIncompleteMultipartUpload"
    }
  }
}

resource "google_bigquery_dataset" "dataset-demo" {
  dataset_id                 = var.bq_dataset_name
  friendly_name              = "test"
  description                = "This is a test description"
  location                   = "US"
  delete_contents_on_destroy = true

}

