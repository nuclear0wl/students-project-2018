FROM python:3

ENV DB_URL sqlite:///foo.db
ADD greetings_app/* /greetings_app/
RUN pip install -r greetings_app/requirements.txt

ENTRYPOINT greetings_app/app.py
