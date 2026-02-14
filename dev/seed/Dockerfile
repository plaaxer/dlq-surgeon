FROM python:3.11-slim

WORKDIR /producer

COPY requirements.txt .

RUN pip install --no-cache-dir -r requirements.txt

COPY . .

RUN chmod +x setup.sh

CMD ["bash", "-c", "./setup.sh && python3 produce_bad_messages.py"]