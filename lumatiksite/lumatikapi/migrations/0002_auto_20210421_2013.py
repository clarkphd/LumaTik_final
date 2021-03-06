# Generated by Django 3.1.7 on 2021-04-21 20:13

from django.db import migrations, models
import django.db.models.deletion
import uuid


class Migration(migrations.Migration):

    dependencies = [
        ('lumatikapi', '0001_initial'),
    ]

    operations = [
        migrations.CreateModel(
            name='Device',
            fields=[
                ('DeviceID', models.IntegerField(primary_key=True, serialize=False)),
                ('Created', models.IntegerField(default=1619035977, editable=False)),
            ],
        ),
        migrations.CreateModel(
            name='Goal',
            fields=[
                ('GoalID', models.UUIDField(default=uuid.uuid4, editable=False, primary_key=True, serialize=False)),
                ('GoalText', models.TextField()),
                ('GoalName', models.TextField()),
            ],
        ),
        migrations.CreateModel(
            name='Reccomend',
            fields=[
                ('ReccomendID', models.UUIDField(default=uuid.uuid4, editable=False, primary_key=True, serialize=False)),
                ('ReccomendText', models.TextField()),
                ('ReccomendName', models.TextField()),
            ],
        ),
        migrations.CreateModel(
            name='User',
            fields=[
                ('UserID', models.IntegerField(primary_key=True, serialize=False)),
                ('Created', models.IntegerField(default=1619035977, editable=False)),
            ],
        ),
        migrations.AddField(
            model_name='data',
            name='UVIndex',
            field=models.FloatField(default=0.1),
            preserve_default=False,
        ),
        migrations.AlterField(
            model_name='data',
            name='UVAval',
            field=models.FloatField(),
        ),
        migrations.AlterField(
            model_name='data',
            name='UVBval',
            field=models.FloatField(),
        ),
        migrations.AlterField(
            model_name='data',
            name='VitDval',
            field=models.FloatField(),
        ),
        migrations.CreateModel(
            name='Wellness',
            fields=[
                ('WellnessDataID', models.UUIDField(default=uuid.uuid4, editable=False, primary_key=True, serialize=False)),
                ('Writetime', models.IntegerField()),
                ('Feeling', models.IntegerField()),
                ('Headache', models.BooleanField()),
                ('BackPain', models.BooleanField()),
                ('NeckAche', models.BooleanField()),
                ('Tired', models.BooleanField()),
                ('MuslePain', models.BooleanField()),
                ('Other', models.TextField()),
                ('DeviceID', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='lumatikapi.device')),
                ('UserID', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='lumatikapi.user')),
            ],
        ),
        migrations.CreateModel(
            name='UserData',
            fields=[
                ('UserDataID', models.UUIDField(default=uuid.uuid4, editable=False, primary_key=True, serialize=False)),
                ('Writetime', models.IntegerField()),
                ('Age', models.IntegerField()),
                ('SkinPigment', models.IntegerField()),
                ('Bedtime', models.IntegerField()),
                ('WakeUp', models.IntegerField()),
                ('Coverage', models.FloatField()),
                ('Location', models.IntegerField()),
                ('DeviceID', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='lumatikapi.device')),
                ('UserID', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='lumatikapi.user')),
            ],
        ),
        migrations.AlterField(
            model_name='data',
            name='DeviceID',
            field=models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='lumatikapi.device'),
        ),
        migrations.AlterField(
            model_name='data',
            name='UserID',
            field=models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='lumatikapi.user'),
        ),
    ]
