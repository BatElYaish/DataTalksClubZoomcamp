import pandas as pd
import os
from sqlalchemy import create_engine
from sqlalchemy.exc import OperationalError
import argparse


def main(params):
    # get env variables
    user = os.getenv('POSTGRES_USER', 'user')
    password = os.getenv('POSTGRES_PASSWORD', 'password')
    host = os.getenv('POSTGRES_HOST', 'localhost')
    port = os.getenv('POSTGRES_PORT', '5432')
    db = os.getenv('POSTGRES_DB', 'db')

    table_name = params.table_name
    url = params.url  # get the data file path

    # create sql engine and connection
    engine = create_engine(f'postgresql://{user}:{password}@{host}:{port}/{db}')
    # Open a connection
    try:
        engine.connect()
    except OperationalError as e:
        print(f"Error connecting to the database: {e}")

    # the backup files are gzipped, and it's important to keep the correct extension
    # for pandas to be able to open the file
    if url.endswith('.csv.gz'):
        csv_name = 'output.csv.gz'
    else:
        csv_name = 'output.csv'

    os.system(f"curl -L {url} -o {csv_name}")

    # we only need first rows to get the schema
    df = pd.read_csv(csv_name, nrows=100, low_memory=False)

    # datetime cols array
    dt_cols = [c for c in df.columns if 'datetime' in c]

    # convert columns to datetime
    for dt_c in dt_cols:
        df[dt_c] = pd.to_datetime(df[dt_c])

    # now we want to only create the table so we'll insert 0 rows of data,
    # if the table exists then drop it and re-create the table
    df.head(n=0).to_sql(name=table_name, con=engine, if_exists='replace')

    # if the file is the zones file load the zones else load the taxi files
    if 'Zone' in df.columns:
        df.to_sql(name='zones', con=engine, if_exists='replace')
        print(f"All rows loaded to {table_name} table.")
    else:
        # set Chunk size
        chunk_size = 100000

        # read the df using an iterator because the data file is big
        df_iter = pd.read_csv(csv_name, iterator=True, chunksize=chunk_size, low_memory=False)

        # load all iterations
        while True:
            try:
                # get the next iteration
                df = next(df_iter)

                # convert columns to datetime
                for dt_c in dt_cols:
                    df[dt_c] = pd.to_datetime(df[dt_c])

                # load data
                df.to_sql(name=table_name, con=engine, if_exists='append')

            except StopIteration:
                print(f"All rows loaded to {table_name} table.")
                break

    # Check if the file exists
    if os.path.exists(csv_name):
        # Delete the file
        os.remove(csv_name)


if __name__ == '__main__':
    # get arguments from command line
    parser = argparse.ArgumentParser(description='Ingest CSV data to Postgres')

    parser.add_argument('--table_name', help='Table to write results to')
    parser.add_argument('--url', help='url of the csv file')

    args = parser.parse_args()
    main(args)
