/*
 * Copyright 2015 Westfälische Hochschule
 *
 * This file is part of Poodle.
 *
 * Poodle is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Poodle is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Poodle.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.whs.poodle.controllers;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/")
public class LoginController {

	@RequestMapping(method = RequestMethod.GET)
	public String login(
			HttpServletRequest request,
			RedirectAttributes redirectAttributes,
			Model model,
			@RequestParam(defaultValue = "0") boolean switchUserFailed) {
		if (switchUserFailed)
			redirectAttributes.addFlashAttribute("errorMessageCode", "userDoesntExist");

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		boolean isLoggedIn = !(auth instanceof AnonymousAuthenticationToken);

		if (!isLoggedIn) // not logged in yet, show login page
			return "login";
		else if (request.isUserInRole("ROLE_STUDENT")) // user is logged in, redirect to start page
			return "redirect:/student";
		else if (request.isUserInRole("ROLE_INSTRUCTOR"))
			return "redirect:/instructor";
		else { // user is logged in, but he is neither student nor instructor (no matching group in LDAP?)
			model.addAttribute("errorMessageCode", "noValidRole");
			return "login";
		}
	}
}
