FROM python:3.7.0b4

LABEL version="1.0"
EXPOSE 5000
ENV DB_URL sqlite:///foo.db
ADD greetings_app/ /greetings_app/
RUN pip install -r greetings_app/requirements.txt

ENTRYPOINT python3 greetings_app/app.py
