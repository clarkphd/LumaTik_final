# Generated by Django 3.1.7 on 2021-04-21 20:14

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('lumatikapi', '0002_auto_20210421_2013'),
    ]

    operations = [
        migrations.AlterField(
            model_name='device',
            name='Created',
            field=models.IntegerField(default=1619036085, editable=False),
        ),
        migrations.AlterField(
            model_name='user',
            name='Created',
            field=models.IntegerField(default=1619036085, editable=False),
        ),
    ]