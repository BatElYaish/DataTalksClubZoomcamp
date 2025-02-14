FROM cluster-base

# -- Layer: JupyterLab
ARG spark_version=3.3.1
ARG jupyterlab_version=3.6.1

# Create a virtual environment and activate it for package installations
RUN python3 -m venv /opt/venv && \
    . /opt/venv/bin/activate && \
    pip install --upgrade pip && \
    pip install --no-cache-dir wget pyspark==${spark_version} jupyterlab==${jupyterlab_version}

# Set the virtual environment's path to be the default
ENV PATH="/opt/venv/bin:$PATH"

# -- Runtime
EXPOSE 8888
WORKDIR ${SHARED_WORKSPACE}
CMD ["jupyter", "lab", "--ip=0.0.0.0", "--port=8888", "--no-browser", "--allow-root", "--NotebookApp.token="]