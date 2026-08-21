# EC2 Setup – en gång

## 1. IAM – OIDC-provider i AWS

```bash
# Skapa OIDC-provider (en gång per AWS-konto)
aws iam create-open-id-connect-provider \
  --url https://token.actions.githubusercontent.com \
  --client-id-list sts.amazonaws.com \
  --thumbprint-list 6938fd4d98bab03faadb97b34396831e3780aea1
```

## 2. IAM-roll för GitHub Actions

Skapa en roll `portplaner-deploy` med trust policy:

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": {
      "Federated": "arn:aws:iam::DITT-KONTO-ID:oidc-provider/token.actions.githubusercontent.com"
    },
    "Action": "sts:AssumeRoleWithWebIdentity",
    "Condition": {
      "StringEquals": {
        "token.actions.githubusercontent.com:aud": "sts.amazonaws.com",
        "token.actions.githubusercontent.com:sub": "repo:plarsa/Portplaner:ref:refs/heads/main"
      }
    }
  }]
}
```

Rollen behöver dessa policies:
- `s3:PutObject` på `arn:aws:s3:::DIN-BUCKET/portplaner/*`
- `ssm:SendCommand` på EC2-instansen
- `ssm:GetCommandInvocation`

## 3. EC2-instansen

```bash
# Skapa användare
sudo useradd -r -s /bin/false portplaner
sudo mkdir -p /opt/portplaner
sudo chown portplaner:portplaner /opt/portplaner

# Kopiera filer
sudo cp deploy.sh /opt/portplaner/deploy.sh
sudo chmod +x /opt/portplaner/deploy.sh
sudo cp portplaner.service /etc/systemd/system/

# Skapa .env med hemligheter (aldrig i git)
sudo nano /opt/portplaner/.env
# Innehåll:
# JWT_SECRET=lång-slumpmässig-sträng
# SPRING_DATASOURCE_URL=jdbc:h2:file:/opt/portplaner/data/portdb

sudo systemctl daemon-reload
sudo systemctl enable portplaner
```

EC2-instansen behöver en **IAM Instance Profile** med:
- `ssm:DescribeInstanceInformation`
- `ssm:UpdateInstanceInformation`  
- `ssmmessages:*`
- `s3:GetObject` på `arn:aws:s3:::DIN-BUCKET/portplaner/*`

SSM Agent måste vara igång: `sudo systemctl enable amazon-ssm-agent`

## 4. GitHub – Environment variables (ej secrets)

Under `Settings → Environments → production`:

| Variable | Exempel |
|----------|---------|
| `AWS_DEPLOY_ROLE_ARN` | `arn:aws:iam::123456789:role/portplaner-deploy` |
| `AWS_REGION` | `eu-north-1` |
| `S3_BUCKET` | `portplaner-releases` |
| `EC2_INSTANCE_ID` | `i-0abc123def456` |

## 5. Branch protection på main

`Settings → Branches → main`:
- ✓ Require pull request before merging
- ✓ Require approvals: 1
- ✓ Require status checks to pass (build)
- ✓ Restrict who can push: [bara dig]
