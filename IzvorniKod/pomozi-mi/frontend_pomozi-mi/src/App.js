import React, { Component } from "react";

import { HashRouter, Switch, Route } from "react-router-dom";

import "./App.css";
import LoginForm from "./components/LoginForm";
import RegisterForm from "./components/RegisterForm";

import LogedInUserComponent from "./components/LogedInUserComponent";

class App extends Component {
	constructor(props) {
		super(props);

		this.state = {
			isLogedIn: sessionStorage.getItem("isLogedIn") || true,
			basicAuthToken: sessionStorage.getItem("basicAuthToken") || "",
		};

		this.setLogInTrue = this.setLogInTrue.bind(this);
		this.setLogInFalse = this.setLogInFalse.bind(this);
	}

	setLogInTrue(usernameXD, passwordCF) {
		this.setState({
			isLogedIn: true,
			basicAuthToken: btoa(
				unescape(encodeURIComponent(usernameXD + ":" + passwordCF))
			),
		});
		let basicAuthToken = btoa(
			unescape(encodeURIComponent(usernameXD + ":" + passwordCF))
		);
		sessionStorage.setItem("isLogedIn", "true");
		sessionStorage.setItem("basicAuthToken", basicAuthToken);
	}

	setLogInFalse() {
		this.setState({
			isLogedIn: false,
			basicAuthToken: "",
		});
		sessionStorage.clear("isLogedIn");
		sessionStorage.clear("basicAuthToken");
	}

	render() {
		if (!this.state.isLogedIn) {
			return (
				<HashRouter>
					<Switch>
						<Route
							exact
							path="/"
							render={() => (
								<LoginForm setLogInTrue={this.setLogInTrue} />
							)}
						/>
						<Route
							exact
							path="/register"
							component={RegisterForm}
						/>
					</Switch>
				</HashRouter>
			);
		} else {
			return <LogedInUserComponent setLogInFalse={this.setLogInFalse} />;
		}
	}
}

export default App;
