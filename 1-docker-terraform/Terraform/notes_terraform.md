# Terraform notes

## Setup Google

1. Install terraform <https://developer.hashicorp.com/terraform/install> AMD64 version, put the file in a designated folder and add to PATH
2. Find the provider on <https://registry.terraform.io/providers>
3. link - <https://registry.terraform.io/providers/hashicorp/google/latest/docs>
4. Click on use provider button and copy the json:

```json
terraform {
  required_providers {
    google = {
      source = "hashicorp/google"
      version = "6.16.0"
    }
  }
}

provider "google" {
  \# Configuration options
}
```

5. copy the configuration json from the same page :

```json
provider "google" {
  project     = "my-project-id"
  region      = "us-central1"
}
```

6. Change to your project and region
7. Run "terraform init", this will create some folders and files

## Bucket configuration

1. to add a bucket search google storage cloud terraform bucket -> <https://registry.terraform.io/providers/wiardvanrij/ipv4google/latest/docs/resources/storage_bucket>
2. copy json

```json
resource "google_storage_bucket" "auto-expire" {
  name          = "auto-expiring-bucket"
  location      = "US"
  force_destroy = true

  lifecycle_rule {
    condition {
      age = 3
    }
    action {
      type = "Delete"
    }
  }
} 
```

```text
`"google_storage_bucket"`: is the name of the resource
`"auto-expire"`: local variable name, using this to access the bucket properties in the tf file
`name`: name of the bucket- needs to be unique in all GCP
`age`: how long the bucket will exist
`action`-> `"Delete"`
`"AbortIncompleteMultipartUpload"` - example, if we have a batch load that takes less than one day, and we see that it takes longer than probably something is wrong with the load and we'd like to abort if the load is incomplete
```

3. Run "terraform plan", this will show you what terraform is about to create
4. Run "terraform apply" to deploy the plan, enter yes to deploy
5. it created a terraform.tfstate file with all the details
6. Run "terraform destroy" to destroy the resources created
7. this will empty out the terraform.tfstate file to basics and create a terraform.tfstate.backup file with the configuration
8. add terraform specific .gitignore file <https://github.com/github/gitignore/blob/main/Terraform.gitignore>
add:

```json
  # Ignore all json files
  *.json
```

in order for git to ignore the credential keys

## Tips

Use `terraform fmt` to prettify your formatting
