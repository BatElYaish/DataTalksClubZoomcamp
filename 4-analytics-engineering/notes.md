# dbt Notes

## dbt Materializations

1. Ephemeral - exists only for the scope of a single dbt run, like temporary tables
2. Views
3. Tables
4. Incremental

    You can can configure in the dbt_project.yaml file how you want all of your project to materialize: view, table or incremental.

    You can also configure it model by model by adding this code at the top of the model:

    ```jinja
    {{ config( materialized='view' ) }}
    ```

## Sources types

1. Sources: External data that is loaded to our model. they are defined in a yaml file. `{{ source('staging', 'data')}}`
2. Seeds: CSV files that are stored in the repo, usually for small data the has a low change rate. like a lookup table. add the csv file under the seeds folder. Run with `dbt seed -s <file_name>`
3. ref: Referencing another table or views in dbt. `{{ ref('stg_data') }}`

## Defining sources

1. create a schema.yml file containing the GCP source and tables

    ```yaml
    sources:
    - name: staging
        database: de-zoomcamp-47 #BQ Project
        schema: de_zoomcamp #BQ Data set 

        tables:
        - name: green_tripdata #BQ table
        - name: yellow_tripdata #BQ table
    ```

    once you have the sources defined, you can change sources only by changing the database and name and not have to change it every time the source is mentioned in the project.
    after you write the tables there will be a line above the table saying `Generate model`

## dbt components

### Macros : Like functions, they are Snippets of SQL code that can be reused in the project. They can also take variables.

> Macro syntax Cheat Sheet: <https://datacoves.com/post/dbt-jinja-cheat-sheet>, <https://datacoves.com/post/dbt-jinja-functions-cheat-sheet>

### Packages: Like libraries, standalone dbt projects with models and macros.

> dbt has it's own package hub at <https://hub.getdbt.com/>. For example - dbt utils <https://hub.getdbt.com/dbt-labs/dbt_utils/latest/>.
> You can also get packages from github using a link to the git repo.
>
> Install dbt packages:
>
>> 1. Create a file called `packages.yml` at the root folder
>> 2. Copy the code to import the package, all the code is imported to the project.
>> 3. You can see that it automatically opened a new folder in root called dbt_packages. If it hasn't try to log out and then og in ,or  run `dbt deps`.
>> 4. Usage - if our package name is dbt_utils, we use it in the sql code as `{{dbt_utils.<macro>()}} as <column_name>`
>>
>> Examples:
>>
>> dbt_utils: can help us with creating an SQL code that will work on any database . For every database we have different ways to cast datetime to date, in dbt we can use `{{ dbt.date_trunc("month", "pickup_datetime") }}`.
>>
>> codegen: can help us generate a yaml of our dbt model so that we don't have to create them manually using `generate_model_yaml` <https://github.com/dbt-labs/dbt-codegen/tree/0.13.1/?tab=readme-ov-file#generate_model_yaml-source>
>> compile this in an empty file and it will generate the schema of the models:
>>
>>```jinja
>>{% set models_to_generate = codegen.get_models(directory='staging', prefix='stg_') %}
>>{{ codegen.generate_model_yaml(
>>    model_names = models_to_generate
>>) }}
>>```

### Testing

Testing can be defined in the schema file for each column . we can decide using the `severity` keyword if we want to get a warning or fail and the threshold for the numbers of tests failed before we get a warning or a fail if the test fails

dbt has 4 built-in testing:

1. unique
2. not null
3. accepted value

    ```yaml
    - accepted_values:
        values:
        - "{{ var('payment_type_values') | join(', ') }}"
        severity: warn
        quote: false
    ```

4. foreign key

    ```yaml
     - relationships:
         field: locationid
         to: ref('taxi_zone_lookup')
    ```

we can also use a condition to define when we want to get a warning and when the number of fails is above a set threshold and we want the test to fail

```yaml
tests:
    - unique:
        config:
        severity: error
        error_if: ">1000"
        warn_if: ">10"
```

### Variables

defining values used across the project

>Variables can be defined in the dbt_project.yaml file.
> Usage examples:
>
>>* a for loop over the values.
>>* Comparing an expected result against the variable
> syntax:
>
>>```yml
>>vars:
>>    payment_type_values: [1, 2, 3 ,4 ,5 ,6]
>>```
>
>Variables we define in the sql code

```jinja
{% if var('is_test_run', default=true) %} limit 100 {% endif %}
```

>Variables defined in CLI:
`dbt build --select <model_name> --vars '{'is_test_run': 'false'}'`

## dbt Commands

`dbt build` builds the whole project. Once one of the models fail it won't continue to build the rest of the models.

`dbt compile` lets you see the final sql code with the jinja code

## dbt deployment

when using the cloud:
deploy-> environments

## dbt Operators

`model+` -The model selected and all models downstream of it  
`+model` -The model selected and all models upstream of it  
n-plus - The model selected and models 'n' nodes upstream/downstream  
`@` -The model selected, all its descendants, and all the ancestors of its descendants  

Source: [dbt Command Guide](https://www.thedataschool.co.uk/edward-hayter/dbt-command-guide/)

## Tips

1. for code suggestions use 2 underscores `__`
