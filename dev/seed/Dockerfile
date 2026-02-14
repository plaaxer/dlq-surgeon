FROM python:3.12-slim

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /producer

COPY seed/requirements.txt .

RUN pip install --no-cache-dir -r requirements.txt

COPY seed/ .
COPY schemas/ schemas/

RUN chmod +x setup.sh

CMD ["bash", "-c", "./setup.sh && python3 produce_bad_messages.py"]