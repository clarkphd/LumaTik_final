# Generated by Django 3.1.7 on 2021-03-16 17:40

from django.db import migrations, models
import uuid


class Migration(migrations.Migration):

    initial = True

    dependencies = [
    ]

    operations = [
        migrations.CreateModel(
            name='Data',
            fields=[
                ('DataID', models.UUIDField(default=uuid.uuid4, editable=False, primary_key=True, serialize=False)),
                ('Writetime', models.IntegerField()),
                ('Rval', models.IntegerField()),
                ('Gval', models.IntegerField()),
                ('Bval', models.IntegerField()),
                ('UVAval', models.IntegerField()),
                ('UVBval', models.IntegerField()),
                ('VitDval', models.IntegerField()),
                ('UserID', models.IntegerField()),
                ('DeviceID', models.IntegerField()),
            ],
        ),
    ]