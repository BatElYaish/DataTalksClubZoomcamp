variable "gcp_cred" {
  description = "GCP SVC cred"
  default     = "./keys/google_creds.json"
}

variable "gcp_project" {
  description = "GCP project id"
  default     = "de-zoomcamp-47"
}

variable "gcp_region" {
  description = "GCP project region"
  default     = "us-central1"
}

variable "gcp_location" {
  description = "GCP resource location"
  default     = "US"
}


variable "gcp_bucket_name" {
  description = "Bucket storage name"
  default     = "de-zoomcamp-47-terra-bucket"
}

variable "gcp_storage_class" {
  description = "Bucket storage class"
  default     = "STANDARD"
}

variable "bq_dataset_name" {
  description = "My big query dataset name"
  default     = "demo_dataset"
}
