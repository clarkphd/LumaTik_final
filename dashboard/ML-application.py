from usergeneration import User,make_n_users


users_list = make_n_users(10000)
df = pd.DataFrame([t.__dict__ for t in users_list ])






