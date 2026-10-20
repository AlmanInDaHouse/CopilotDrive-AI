FROM python:3.12-slim
WORKDIR /opt/opencar
RUN pip install --no-cache-dir pyserial==3.5
COPY app/ ./app/
COPY web/ ./web/
ENV HOST=0.0.0.0 PORT=8787 TELEMETRY_MODE=simulator PYTHONUNBUFFERED=1
USER 65534:65534
EXPOSE 8787
CMD ["python", "-m", "app.server"]
