import React, { useState, useEffect } from "react";
import { Button, Icon, Divider } from "semantic-ui-react";
import { Modal, ModalHeader, ModalBody, ModalFooter } from "reactstrap";

const baseUrl = "http://localhost:8080";

function PotentialUsers(props) {
	const [list, setList] = React.useState([]);
	const [modal, setModal] = useState(false);
	const toggle = () => setModal(!modal);

   //Funkcija za prihvat potencijalnog korisnika
	async function acceptUser(user) {
		const newList = list.filter((item) => item.username !== user.username);
		setList(newList);

		var myHeaders = new Headers();
		myHeaders.append(
			"Authorization",
			"Basic " + sessionStorage.getItem("basicAuthToken")
		);
		myHeaders.append("Content-Type", "application/json");

		const options = {
			method: "POST",
			headers: myHeaders,
			body: JSON.stringify(user),
		};

		await fetch(
			baseUrl + `/requests/pickHandler/${props.id}`,
			options
		).then((response) => {
			if (response.status === 200) {
            console.log("Uspješano prihvaćanje");
            window.location.reload(false);
            toggle();
			} else {
				console.log("Neuspješano prihvaćanje");
			}
		});
	}

	//Funkcija za uklanjanje potencijalnog korisnika
	async function handleRemove(user) {
		const newList = list.filter((item) => item.username !== user.username);
		setList(newList);

		var myHeaders = new Headers();
		myHeaders.append(
			"Authorization",
			"Basic " + sessionStorage.getItem("basicAuthToken")
		);
		myHeaders.append("Content-Type", "application/json");

		const options = {
			method: "POST",
			headers: myHeaders,
			body: JSON.stringify(user),
		};

		await fetch(
			baseUrl + `/requests/rejectHandler/${props.id}`,
			options
		).then((response) => {
			if (response.status === 200) {
            console.log("Uspješano odbijanje");
			} else {
				console.log("Neuspješano odbijanje");
			}
		});
	}

	//Dohvat potencijalnih hanldera
	useEffect(() => {
		var myHeaders = new Headers();

		myHeaders.append(
			"Authorization",
			"Basic " + sessionStorage.getItem("basicAuthToken")
		);

		const options = {
			method: "GET",
			headers: myHeaders,
		};

		fetch(baseUrl + `/requests/getPotentialHandlers/${props.id}`, options)
			.then((response) => response.text())
			.then((result) => setList(JSON.parse(result)))
			.catch((error) => console.log("error", error));
	}, []);

	return (
		<div>
			<Button color="blue" size="large" floated="right" onClick={toggle}>
				Notice me senpai{" "}
			</Button>{" "}
			<Modal isOpen={modal} toggle={toggle}>
				<ModalHeader toggle={toggle}>
					Potencijalni izvršitelji{" "}
				</ModalHeader>{" "}
				<ModalBody>
					{" "}
					{list.map((item) => (
						<div key={item.username}>
							<Icon name="user" /> @{item.username} {" | "}{" "}
							{item.firstName} {item.lastName}{" "}
							<Button
								color="red"
								floated="right"
								size="medium"
								onClick={() => handleRemove(item)}
							>Odbij</Button>{" "}
							<Button
								color="green"
								floated="right"
                        size="medium"
                        onClick={() => acceptUser(item)}
							>Prihvati</Button>
							<Divider />
						</div>
					))}{" "}
				</ModalBody>{" "}
			</Modal>{" "}
		</div>
	);
}

export default PotentialUsers;